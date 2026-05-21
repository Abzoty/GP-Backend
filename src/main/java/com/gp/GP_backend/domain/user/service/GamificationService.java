package com.gp.GP_backend.domain.user.service;

import com.gp.GP_backend.domain.material.repository.MaterialRepository;
import com.gp.GP_backend.domain.post.repository.AnswerRepository;
import com.gp.GP_backend.domain.post.repository.PostRepository;
import com.gp.GP_backend.domain.space.entity.SpaceMembership;
import com.gp.GP_backend.domain.space.repository.SpaceMembershipRepository;
import com.gp.GP_backend.domain.space.repository.SpaceRepository;
import com.gp.GP_backend.domain.user.dto.GamificationProfileResponse;
import com.gp.GP_backend.domain.user.dto.SpaceLeaderboardEntry;
import com.gp.GP_backend.domain.user.dto.SystemLeaderboardEntry;
import com.gp.GP_backend.domain.user.entity.GamificationProfile;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.entity.XpTransaction;
import com.gp.GP_backend.domain.user.repository.GamificationProfileRepository;
import com.gp.GP_backend.domain.user.repository.UserRepository;
import com.gp.GP_backend.domain.user.repository.XpTransactionRepository;
import com.gp.GP_backend.shared.exception.ApiException;
import com.gp.GP_backend.shared.util.XpCalculator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Manages XP awards, level-ups, streak tracking, profile retrieval,
 * and leaderboard generation for the gamification system.
 *
 * <p>
 * All write methods are {@code @Transactional} and participate in the
 * calling service's transaction (propagation = REQUIRED by default), so XP
 * changes are always atomic with the action that triggered them.
 *
 * <p>
 * <b>Design notes:</b>
 * <ul>
 * <li>Award methods accept a {@code UUID userId} rather than a {@code User}
 * entity to avoid detached-entity issues when called from non-transactional
 * callers (e.g. AuthController login). The user FK in
 * {@link XpTransaction} is set via
 * {@link UserRepository#getReferenceById}, which yields a Hibernate
 * proxy without issuing an extra SELECT.</li>
 * <li>The {@link GamificationProfile} is created lazily on first XP award if
 * it does not already exist, providing a safe fallback for users that
 * pre-date the feature.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationService {

    private final GamificationProfileRepository gamificationProfileRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;
    private final SpaceMembershipRepository spaceMembershipRepository;
    private final PostRepository postRepository;
    private final AnswerRepository answerRepository;
    private final MaterialRepository materialRepository;
     // Injected separately — EntityManager is transaction-scoped,
    // not a singleton bean. @PersistenceContext provides a thread-safe
    // proxy that routes to the correct instance per transaction.
    @PersistenceContext
    private EntityManager entityManager;

    // ─── Profile bootstrap ────────────────────────────────────────────────────

    /**
        * Creates a blank {@link GamificationProfile} for a user.
        *
        * <p>
        * Note: profiles are normally created lazily on first gamification interaction.
        * This method is kept as an optional bootstrap utility (e.g., migrations or
        * future eager-creation at registration).
     *
     * @param user the freshly persisted user entity.
     */
    @Transactional
    public void createProfileForUser(User user) {
        GamificationProfile profile = GamificationProfile.builder()
                .user(user)
                .build();
        gamificationProfileRepository.save(profile);
        log.debug("Gamification profile created for user {}", user.getId());
    }

    // ─── XP awards ────────────────────────────────────────────────────────────

    /**
     * Awards XP to a user for a given action with no associated entity reference.
     *
     * @param userId    the recipient's UUID.
     * @param eventType one of the {@code XpCalculator.EVENT_*} constants.
     * @param xpDelta   positive integer; XP to add.
     */
    @Transactional
    public void awardXp(UUID userId, String eventType, int xpDelta) {
        awardXp(userId, eventType, xpDelta, null, null);
    }

    /**
     * Awards XP to a user for a given action, recording a polymorphic reference
     * to the triggering entity (post, answer, material, …).
     *
     * <p>
     * Steps:
     * <ol>
     * <li>Load (or lazily create) the user's {@link GamificationProfile}.</li>
     * <li>Increment {@code xpPoints} and recalculate {@code level}.</li>
     * <li>Increment the relevant denormalised counter on the profile.</li>
     * <li>Persist an immutable {@link XpTransaction} audit record.</li>
     * </ol>
     *
     * @param userId        the recipient's UUID.
     * @param eventType     one of the {@code XpCalculator.EVENT_*} constants.
     * @param xpDelta       positive integer; XP to add.
     * @param referenceId   UUID of the entity that triggered this event
     *                      (nullable).
     * @param referenceType one of the {@code XpCalculator.REF_*} constants
     *                      (nullable).
     */
    @Transactional
    public void awardXp(UUID userId,
            String eventType,
            int xpDelta,
            UUID referenceId,
            String referenceType) {

        if (xpDelta <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "xpDelta must be positive for XP awards");
        }

        String eventKey = buildEventKey(userId, eventType, referenceType, referenceId, null);

        // Idempotency guard: insert the transaction first. If it's a duplicate,
        // skip the award entirely.
        if (!tryAppendTransaction(userId, eventType, eventKey, xpDelta, referenceId, referenceType)) {
            return;
        }

        GamificationProfile profile = getOrCreateProfileForWrite(userId);

        int newXp = profile.getXpPoints() + xpDelta;
        profile.setXpPoints(newXp);
        profile.setLevel(XpCalculator.calculateLevel(newXp));

        switch (eventType) {
            case XpCalculator.EVENT_POST_CREATED ->
                    profile.setTotalPosts(profile.getTotalPosts() + 1);
            case XpCalculator.EVENT_ANSWER_GIVEN ->
                    profile.setTotalAnswers(profile.getTotalAnswers() + 1);
            case XpCalculator.EVENT_ANSWER_UPVOTED ->
                    profile.setTotalUpvotesReceived(profile.getTotalUpvotesReceived() + 1);
            case XpCalculator.EVENT_MATERIAL_SHARED ->
                    profile.setTotalMaterialsShared(profile.getTotalMaterialsShared() + 1);
            default -> {
            }
        }

        gamificationProfileRepository.save(profile);

        log.debug("Awarded {} XP ({}) to user {}", xpDelta, eventType, userId);
    }

    @Transactional
public void revokeXp(UUID userId,
                     String eventType,
                     UUID referenceId,
                     String referenceType) {

    String eventKey = buildEventKey(userId, eventType, referenceType, referenceId, null);

    Optional<XpTransaction> txOpt = xpTransactionRepository.findByEventKey(eventKey);

    if (txOpt.isEmpty()) {
        return; // already revoked or never existed → idempotent
    }

    XpTransaction tx = txOpt.get();

    GamificationProfile profile = getOrCreateProfileForWrite(userId);

    int xpDelta = tx.getXpDelta();

    int newXp = profile.getXpPoints() - xpDelta;
    profile.setXpPoints(Math.max(0, newXp)); // avoid negative XP
    profile.setLevel(XpCalculator.calculateLevel(profile.getXpPoints()));

    // 🔻 Reverse counters
    switch (eventType) {
        case XpCalculator.EVENT_POST_CREATED ->
                profile.setTotalPosts(Math.max(0, profile.getTotalPosts() - 1));
        case XpCalculator.EVENT_ANSWER_GIVEN ->
                profile.setTotalAnswers(Math.max(0, profile.getTotalAnswers() - 1));
        case XpCalculator.EVENT_ANSWER_UPVOTED ->
                profile.setTotalUpvotesReceived(Math.max(0, profile.getTotalUpvotesReceived() - 1));
        case XpCalculator.EVENT_MATERIAL_SHARED ->
                profile.setTotalMaterialsShared(Math.max(0, profile.getTotalMaterialsShared() - 1));
        default -> {}
    }

    gamificationProfileRepository.save(profile);

    // 🔥 remove transaction (so it can be awarded again later if needed)
    xpTransactionRepository.delete(tx);

    log.debug("Revoked {} XP ({}) from user {}", xpDelta, eventType, userId);
}
    // ─── Daily login + streak tracking ───────────────────────────────────────

    /**
     * Records a daily login event: awards {@link XpCalculator#XP_DAILY_LOGIN}
     * XP once per calendar day, updates the consecutive-day streak, and grants
     * a {@link XpCalculator#XP_STREAK_BONUS} on milestone days
     * (7, 15, 30, 100).
     *
     * <p>
     * Calling this method more than once on the same calendar day is a
     * no-op — the guard on {@code lastActivityDate} prevents duplicate awards.
     *
     * @param userId the authenticated user's UUID.
     */
    @Transactional
    public void trackDailyLogin(UUID userId) {
                LocalDate today = LocalDate.now();

                GamificationProfile profile = getOrCreateProfileForWrite(userId);
                LocalDate lastActivity = profile.getLastActivityDate();

                if (today.equals(lastActivity)) {
                        return;
                }

                // Insert the DAILY_LOGIN transaction with a per-day idempotency key.
                String dailyKey = buildEventKey(userId, XpCalculator.EVENT_DAILY_LOGIN,
                                XpCalculator.REF_LOGIN, null, today.toString());

                if (!tryAppendTransaction(userId, XpCalculator.EVENT_DAILY_LOGIN, dailyKey,
                                XpCalculator.XP_DAILY_LOGIN, null, XpCalculator.REF_LOGIN)) {
                        return;
                }

                // Update streak
                if (lastActivity != null && lastActivity.equals(today.minusDays(1))) {
                        profile.setCurrentStreakDays((short) (profile.getCurrentStreakDays() + 1));
                } else {
                        profile.setCurrentStreakDays((short) 1);
                }

                if (profile.getCurrentStreakDays() > profile.getLongestStreakDays()) {
                        profile.setLongestStreakDays(profile.getCurrentStreakDays());
                }

                profile.setLastActivityDate(today);

                int xpAfterDaily = profile.getXpPoints() + XpCalculator.XP_DAILY_LOGIN;
                profile.setXpPoints(xpAfterDaily);
                profile.setLevel(XpCalculator.calculateLevel(xpAfterDaily));

                // Milestone bonus: append-only transaction, idempotent per day+milestone.
                if (XpCalculator.isStreakMilestone(profile.getCurrentStreakDays())) {
                        String bonusKey = buildEventKey(userId, XpCalculator.EVENT_STREAK_BONUS,
                                        XpCalculator.REF_LOGIN, null,
                                        today + "|" + profile.getCurrentStreakDays());

                        if (tryAppendTransaction(userId, XpCalculator.EVENT_STREAK_BONUS, bonusKey,
                                        XpCalculator.XP_STREAK_BONUS, null, XpCalculator.REF_LOGIN)) {
                                int xpAfterBonus = profile.getXpPoints() + XpCalculator.XP_STREAK_BONUS;
                                profile.setXpPoints(xpAfterBonus);
                                profile.setLevel(XpCalculator.calculateLevel(xpAfterBonus));
                        }
                }

                gamificationProfileRepository.save(profile);
    }

        // ─── Internal helpers ───────────────────────────────────────────────────

        private GamificationProfile getOrCreateProfile(UUID userId) {
                return gamificationProfileRepository.findByUserId(userId)
                                .orElseGet(() -> {
                                        User userRef = userRepository.getReferenceById(userId);
                                        GamificationProfile created = GamificationProfile.builder().user(userRef).build();
                                        try {
                                                return gamificationProfileRepository.save(created);
                                        } catch (DataIntegrityViolationException ex) {
                                                // Another concurrent request inserted the profile first.
                                                return gamificationProfileRepository.findByUserId(userId)
                                                                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                                                                                "Failed to create gamification profile"));
                                        }
                                });
        }

        private GamificationProfile getOrCreateProfileForWrite(UUID userId) {
                return gamificationProfileRepository.findByUserIdForUpdate(userId)
                                .orElseGet(() -> {
                                        User userRef = userRepository.getReferenceById(userId);

                                        // Insert if missing, then take a pessimistic lock on the managed entity.
                                        // If another request wins the insert race, fall back to the locked read path.
                                        GamificationProfile created = GamificationProfile.builder().user(userRef).build();
                                        try {
                                                created = gamificationProfileRepository.saveAndFlush(created);
                                        } catch (DataIntegrityViolationException ex) {
                                                return gamificationProfileRepository.findByUserIdForUpdate(userId)
                                                                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                                                                                "Failed to create gamification profile"));
                                        }

                                        if (entityManager != null) {
                                                entityManager.lock(created, LockModeType.PESSIMISTIC_WRITE);
                                        }
                                        return created;
                                });
        }

        private boolean tryAppendTransaction(UUID userId,
                        String eventType,
                        String eventKey,
                        int xpDelta,
                        UUID referenceId,
                        String referenceType) {

                // Best-effort fast-path: avoids a known-duplicate insert.
                // This is NOT a correctness guarantee under concurrency; the real guard is the
                // DB unique constraint on xp_transactions.event_key + the catch below.
                if (xpTransactionRepository.existsByEventKey(eventKey)) {
                        return false;
                }

                try {
                        xpTransactionRepository.saveAndFlush(
                                        XpTransaction.builder()
                                                        .user(userRepository.getReferenceById(userId))
                                                        .eventType(eventType)
                                                        .eventKey(eventKey)
                                                        .xpDelta(xpDelta)
                                                        .referenceId(referenceId)
                                                        .referenceType(referenceType)
                                                        .build());
                        return true;
                } catch (DataIntegrityViolationException ex) {
                        return false;
                }
        }

        private String buildEventKey(UUID userId,
                        String eventType,
                        String referenceType,
                        UUID referenceId,
                        String extra) {

                // '|' is a safe separator here: UUID.toString() and our constant tokens don't contain it.
                // The key is used for uniqueness/idempotency (not parsing), so `extra` may contain '|'.
                String refType = (referenceType == null || referenceType.isBlank()) ? "-" : referenceType;
                String refId = (referenceId == null) ? "-" : referenceId.toString();
                String suffix = (extra == null || extra.isBlank()) ? "" : ("|" + extra);
                return userId + "|" + eventType + "|" + refType + "|" + refId + suffix;
        }

    // ─── Profile retrieval ────────────────────────────────────────────────────

    /**
     * Returns the full gamification profile for the given user.
     *
     * @throws ApiException 404 if the user has no gamification profile.
     */
    @Transactional(readOnly = true)
    public GamificationProfileResponse getProfile(UUID userId) {
        GamificationProfile profile = gamificationProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Gamification profile not found for user: " + userId));
        return toProfileResponse(profile);
    }

    // ─── Leaderboards ─────────────────────────────────────────────────────────

    /**
     * Returns the top {@code limit} users platform-wide, ranked by overall XP
     * (descending), with ties broken by level.
     *
     * @param limit maximum number of entries to return (capped at 100).
     */
    @Transactional(readOnly = true)
    public List<SystemLeaderboardEntry> getSystemLeaderboard(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<GamificationProfile> profiles = gamificationProfileRepository
                .findTopWithUserOrderByXpDesc(PageRequest.of(0, safeLimit));

        AtomicInteger rank = new AtomicInteger(1);
        return profiles.stream()
                .map(p -> SystemLeaderboardEntry.builder()
                        .rank(rank.getAndIncrement())
                        .userId(p.getUser().getId())
                        .fullName(p.getUser().getFullName())
                        .xpPoints(p.getXpPoints())
                        .level(p.getLevel())
                        .totalPosts(p.getTotalPosts())
                        .totalAnswers(p.getTotalAnswers())
                        .totalMaterialsShared(p.getTotalMaterialsShared())
                        .build())
                .toList();
    }

    /**
     * Returns all members of {@code spaceId}, ranked by their <em>overall</em>
     * XP (descending), with space-scoped activity counters.
     *
     * <p>
     * Only authenticated members of the space may request this leaderboard.
     *
     * @param spaceId     the target space.
     * @param requesterId the authenticated user making the request.
     * @param limit       maximum number of entries (capped at 100).
     * @throws ApiException 404 if the space does not exist.
     * @throws ApiException 403 if the requester is not a member of the space.
     */
    @Transactional(readOnly = true)
    public List<SpaceLeaderboardEntry> getSpaceLeaderboard(UUID spaceId,
            UUID requesterId,
            int limit) {
        if (!spaceRepository.existsById(spaceId)) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "Space not found with id: " + spaceId);
        }
        if (!spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, requesterId)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "You must be a member of this space to view its leaderboard");
        }

        // Load all members with their User associations in one query
        List<SpaceMembership> memberships = spaceMembershipRepository.findBySpace_Id(spaceId);

        List<UUID> memberIds = memberships.stream()
                .map(m -> m.getUser().getId())
                .toList();

        // Bulk-load gamification profiles (single query, JOIN FETCH user)
        Map<UUID, GamificationProfile> profileMap = gamificationProfileRepository.findByUserIdsWithUser(memberIds)
                .stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));

//  ADD — bulk-load all counts in 3 queries (not N×3)
Map<UUID, Long> postCounts = postRepository
        .countBySpaceIdGroupByAuthor(spaceId)
        .stream()
        .collect(Collectors.toMap(
                row -> (UUID) row[0],
                row -> (Long)  row[1]));

Map<UUID, Long> answerCounts = answerRepository
        .countBySpaceIdGroupByAuthor(spaceId)
        .stream()
        .collect(Collectors.toMap(
                row -> (UUID) row[0],
                row -> (Long)  row[1]));

Map<UUID, Long> materialCounts = materialRepository
        .countBySpaceIdGroupByUploader(spaceId)
        .stream()
        .collect(Collectors.toMap(
                row -> (UUID) row[0],
                row -> (Long)  row[1]));

int safeLimit = Math.max(1, Math.min(limit, 100));

//  CHANGE — the .map() now reads from the pre-built maps (no per-member queries)
List<SpaceLeaderboardEntry> entries = memberships.stream()
        .map(m -> {
            UUID uid = m.getUser().getId();
            GamificationProfile profile = profileMap.get(uid);

            return SpaceLeaderboardEntry.builder()
                    .userId(uid)
                    .fullName(m.getUser().getFullName())
                    .xpPoints(profile != null ? profile.getXpPoints() : 0)
                    .level(profile != null ? profile.getLevel() : (short) 1)
                    // reads from map — zero queries here
                    .postsInSpace(postCounts.getOrDefault(uid, 0L).intValue())
                    .answersInSpace(answerCounts.getOrDefault(uid, 0L).intValue())
                    .materialsSharedInSpace(materialCounts.getOrDefault(uid, 0L).intValue())
                    .build();
        })
        .sorted(Comparator
                .comparingInt(SpaceLeaderboardEntry::getXpPoints).reversed()
                .thenComparing(Comparator.comparingInt(e -> -e.getLevel())))
        .limit(safeLimit)
        .collect(Collectors.toList());

        // Assign 1-based ranks after sorting
        AtomicInteger rank = new AtomicInteger(1);
        entries.forEach(e -> e.setRank(rank.getAndIncrement()));

        return entries;
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Loads the gamification profile for a user, or lazily creates one if none
     * exists (safe fallback for users that pre-date the gamification feature).
     */
//     private GamificationProfile getOrCreateProfile(UUID userId) {
//         return gamificationProfileRepository.findByUserId(userId)
//                 .orElseGet(() -> {
//                     GamificationProfile fresh = GamificationProfile.builder()
//                             .user(userRepository.getReferenceById(userId))
//                             .build();
//                     try {
//                         GamificationProfile saved = gamificationProfileRepository.save(fresh);
//                         log.debug("Lazily created gamification profile for user {}", userId);
//                         return saved;
//                     } catch (DataIntegrityViolationException ex) {
//                         // Another transaction created it first.
//                         return gamificationProfileRepository.findByUserId(userId)
//                                 .orElseThrow(() -> ex);
//                     }
//                 });
//     }

    /** Maps a {@link GamificationProfile} to its response DTO. */
    private GamificationProfileResponse toProfileResponse(GamificationProfile p) {
        return GamificationProfileResponse.builder()
                .userId(p.getUser().getId())
                .xpPoints(p.getXpPoints())
                .level(p.getLevel())
                .totalPosts(p.getTotalPosts())
                .totalAnswers(p.getTotalAnswers())
                .totalUpvotesReceived(p.getTotalUpvotesReceived())
                .totalMaterialsShared(p.getTotalMaterialsShared())
                .currentStreakDays(p.getCurrentStreakDays())
                .longestStreakDays(p.getLongestStreakDays())
                .lastActivityDate(p.getLastActivityDate())
                .build();
    }
}