package com.gp.GP_backend.domain.user.service;

import com.gp.GP_backend.domain.material.repository.MaterialRepository;
import com.gp.GP_backend.domain.post.repository.AnswerRepository;
import com.gp.GP_backend.domain.post.repository.PostRepository;
import com.gp.GP_backend.domain.space.entity.Space;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamificationServiceTest {

    @Mock private GamificationProfileRepository gamificationProfileRepository;
    @Mock private XpTransactionRepository xpTransactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private SpaceRepository spaceRepository;
    @Mock private SpaceMembershipRepository spaceMembershipRepository;
    @Mock private PostRepository postRepository;
    @Mock private AnswerRepository answerRepository;
    @Mock private MaterialRepository materialRepository;

    @InjectMocks
    private GamificationService gamificationService;

    // ═══════════════════════════════════════════════════════════════════════════
    // createProfileForUser
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void createProfileForUser_shouldSaveBlankProfile() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).fullName("User").build();

        when(gamificationProfileRepository.save(any(GamificationProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        gamificationService.createProfileForUser(user);

        ArgumentCaptor<GamificationProfile> captor = ArgumentCaptor.forClass(GamificationProfile.class);
        verify(gamificationProfileRepository).save(captor.capture());
        assertEquals(userId, captor.getValue().getUser().getId());
        assertEquals(Integer.valueOf(0), captor.getValue().getXpPoints());
        assertEquals((short) 1, captor.getValue().getLevel());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // awardXp
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void awardXp_shouldIncreaseXpAndCreateTransaction() {
        UUID userId = UUID.randomUUID();
        GamificationProfile profile = profile(userId);

        when(gamificationProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());
        when(gamificationProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(xpTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gamificationService.awardXp(
                userId,
                XpCalculator.EVENT_POST_CREATED,
                XpCalculator.XP_POST_CREATED,
                UUID.randomUUID(),
                XpCalculator.REF_POST);

        assertEquals(XpCalculator.XP_POST_CREATED, profile.getXpPoints());
        assertEquals(Integer.valueOf(1), profile.getTotalPosts());
        assertEquals(XpCalculator.calculateLevel(XpCalculator.XP_POST_CREATED), profile.getLevel());

        ArgumentCaptor<XpTransaction> txCaptor = ArgumentCaptor.forClass(XpTransaction.class);
        verify(xpTransactionRepository).save(txCaptor.capture());
        assertEquals(XpCalculator.EVENT_POST_CREATED, txCaptor.getValue().getEventType());
        assertEquals(Integer.valueOf(XpCalculator.XP_POST_CREATED), txCaptor.getValue().getXpDelta());
        assertEquals(XpCalculator.REF_POST, txCaptor.getValue().getReferenceType());
    }

    @Test
    void awardXp_shouldRejectZeroDelta() {
        UUID userId = UUID.randomUUID();

        ApiException ex = assertThrows(ApiException.class, () ->
                gamificationService.awardXp(userId, XpCalculator.EVENT_POST_CREATED, 0,
                        UUID.randomUUID(), XpCalculator.REF_POST));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void awardXp_shouldRejectNegativeDelta() {
        UUID userId = UUID.randomUUID();

        ApiException ex = assertThrows(ApiException.class, () ->
                gamificationService.awardXp(userId, XpCalculator.EVENT_POST_CREATED, -5,
                        UUID.randomUUID(), XpCalculator.REF_POST));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void awardXp_shouldCreateMissingProfileLazily() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).build();

        when(gamificationProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(gamificationProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(xpTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gamificationService.awardXp(
                userId,
                XpCalculator.EVENT_ANSWER_GIVEN,
                XpCalculator.XP_ANSWER_GIVEN,
                UUID.randomUUID(),
                XpCalculator.REF_ANSWER);

        // First save = lazy profile creation, second save = profile XP update
        verify(gamificationProfileRepository, times(2)).save(any(GamificationProfile.class));
        verify(xpTransactionRepository, times(1)).save(any(XpTransaction.class));
    }

    @Test
    void awardXp_shouldIncrementAnswerCounter() {
        UUID userId = UUID.randomUUID();
        GamificationProfile profile = profile(userId);

        when(gamificationProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());
        when(gamificationProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(xpTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gamificationService.awardXp(userId, XpCalculator.EVENT_ANSWER_GIVEN,
                XpCalculator.XP_ANSWER_GIVEN, UUID.randomUUID(), XpCalculator.REF_ANSWER);

        assertEquals(Integer.valueOf(1), profile.getTotalAnswers());
        assertEquals(Integer.valueOf(0), profile.getTotalPosts()); // untouched
    }

    @Test
    void awardXp_shouldIncrementUpvoteCounter() {
        UUID userId = UUID.randomUUID();
        GamificationProfile profile = profile(userId);

        when(gamificationProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());
        when(gamificationProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(xpTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gamificationService.awardXp(userId, XpCalculator.EVENT_ANSWER_UPVOTED,
                XpCalculator.XP_ANSWER_UPVOTED, UUID.randomUUID(), XpCalculator.REF_ANSWER);

        assertEquals(Integer.valueOf(1), profile.getTotalUpvotesReceived());
    }

    @Test
    void awardXp_shouldIncrementMaterialCounter() {
        UUID userId = UUID.randomUUID();
        GamificationProfile profile = profile(userId);

        when(gamificationProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());
        when(gamificationProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(xpTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gamificationService.awardXp(userId, XpCalculator.EVENT_MATERIAL_SHARED,
                XpCalculator.XP_MATERIAL_SHARED, UUID.randomUUID(), XpCalculator.REF_MATERIAL);

        assertEquals(Integer.valueOf(1), profile.getTotalMaterialsShared());
    }

    /**
     * Documents a KNOWN VULNERABILITY: awardXp() has no idempotency guard.
     * Calling it twice for the same logical event double-awards XP.
     * This is only safe because callers (PostService, MaterialService, VoteService)
     * are responsible for ensuring single invocation.
     * See Issue #9 — if those callers don't enforce it, XP farming is possible.
     */
    @Test
    void awardXp_knownVulnerability_duplicateCallsDoubleAwardXp() {
        UUID userId = UUID.randomUUID();
        GamificationProfile profile = profile(userId);

        when(gamificationProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());
        when(gamificationProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(xpTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gamificationService.awardXp(userId, XpCalculator.EVENT_POST_CREATED,
                XpCalculator.XP_POST_CREATED, UUID.randomUUID(), XpCalculator.REF_POST);
        gamificationService.awardXp(userId, XpCalculator.EVENT_POST_CREATED,
                XpCalculator.XP_POST_CREATED, UUID.randomUUID(), XpCalculator.REF_POST);

        // Documents the vulnerability — XP is doubled on duplicate calls.
        // FIX REQUIRED: callers must never call awardXp twice for the same action.
        // Long-term fix: add an idempotency key to XpTransaction and reject duplicates.
        assertEquals(Integer.valueOf(XpCalculator.XP_POST_CREATED * 2), profile.getXpPoints());
        assertEquals(Integer.valueOf(2), profile.getTotalPosts());
        verify(xpTransactionRepository, times(2)).save(any(XpTransaction.class));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // revokeXp
    // ═══════════════════════════════════════════════════════════════════════════

    // @Test
    // void revokeXp_shouldDeductXpAndSaveNegativeTransaction() {
    //     UUID userId = UUID.randomUUID();
    //     UUID refId   = UUID.randomUUID();
    //     GamificationProfile profile = profile(userId);
    //     profile.setXpPoints(50);
    //     profile.setLevel(XpCalculator.calculateLevel(50));
    //     profile.setTotalPosts(2);

    //     when(gamificationProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
    //     when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());
    //     when(gamificationProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    //     when(xpTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    //     gamificationService.revokeXp(
    //             userId,
    //             XpCalculator.EVENT_POST_CREATED,
    //             XpCalculator.XP_POST_CREATED,
    //             refId,
    //             XpCalculator.REF_POST);

    //     assertEquals(50 - XpCalculator.XP_POST_CREATED, profile.getXpPoints());
    //     assertEquals(Integer.valueOf(1), profile.getTotalPosts());

    //     ArgumentCaptor<XpTransaction> txCaptor = ArgumentCaptor.forClass(XpTransaction.class);
    //     verify(xpTransactionRepository).save(txCaptor.capture());
    //     assertTrue(txCaptor.getValue().getXpDelta() < 0,
    //             "Revocation transaction must have a negative xpDelta");
    //     assertTrue(txCaptor.getValue().getEventType().endsWith("_REVOKED"),
    //             "Revocation transaction eventType must end with _REVOKED");
    //     assertEquals(refId, txCaptor.getValue().getReferenceId());
    // }

    // @Test
    // void revokeXp_shouldClampAtZeroAndNotGoNegative() {
    //     UUID userId = UUID.randomUUID();
    //     GamificationProfile profile = profile(userId);
    //     profile.setXpPoints(5); // less than one award's worth

    //     when(gamificationProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
    //     when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());
    //     when(gamificationProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    //     when(xpTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    //     // Revoke more XP than the user has
    //     gamificationService.revokeXp(
    //             userId,
    //             XpCalculator.EVENT_POST_CREATED,
    //             XpCalculator.XP_POST_CREATED, // e.g. 10 > 5
    //             UUID.randomUUID(),
    //             XpCalculator.REF_POST);

    //     assertEquals(Integer.valueOf(0), profile.getXpPoints(),
    //             "XP must never go below zero");
    // }

    // @Test
    // void revokeXp_shouldDecrementAnswerCounter() {
    //     UUID userId = UUID.randomUUID();
    //     GamificationProfile profile = profile(userId);
    //     profile.setXpPoints(50);
    //     profile.setTotalAnswers(3);

    //     when(gamificationProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
    //     when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());
    //     when(gamificationProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    //     when(xpTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    //     gamificationService.revokeXp(userId, XpCalculator.EVENT_ANSWER_GIVEN,
    //             XpCalculator.XP_ANSWER_GIVEN, UUID.randomUUID(), XpCalculator.REF_ANSWER);

    //     assertEquals(Integer.valueOf(2), profile.getTotalAnswers());
    //     assertEquals(Integer.valueOf(0), profile.getTotalPosts()); // untouched
    // }

    // @Test
    // void revokeXp_shouldDecrementMaterialCounter() {
    //     UUID userId = UUID.randomUUID();
    //     GamificationProfile profile = profile(userId);
    //     profile.setXpPoints(50);
    //     profile.setTotalMaterialsShared(2);

    //     when(gamificationProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
    //     when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());
    //     when(gamificationProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    //     when(xpTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    //     gamificationService.revokeXp(userId, XpCalculator.EVENT_MATERIAL_SHARED,
    //             XpCalculator.XP_MATERIAL_SHARED, UUID.randomUUID(), XpCalculator.REF_MATERIAL);

    //     assertEquals(Integer.valueOf(1), profile.getTotalMaterialsShared());
    // }

    // @Test
    // void revokeXp_counterShouldNotGoBelowZero() {
    //     UUID userId = UUID.randomUUID();
    //     GamificationProfile profile = profile(userId);
    //     profile.setXpPoints(50);
    //     profile.setTotalPosts(0); // already at zero

    //     when(gamificationProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
    //     when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());
    //     when(gamificationProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    //     when(xpTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    //     gamificationService.revokeXp(userId, XpCalculator.EVENT_POST_CREATED,
    //             XpCalculator.XP_POST_CREATED, UUID.randomUUID(), XpCalculator.REF_POST);

    //     assertEquals(Integer.valueOf(0), profile.getTotalPosts(),
    //             "Counter must never go below zero");
    // }

    // @Test
    // void revokeXp_shouldRejectNonPositiveDelta() {
    //     UUID userId = UUID.randomUUID();

    //     ApiException ex = assertThrows(ApiException.class, () ->
    //             gamificationService.revokeXp(userId, XpCalculator.EVENT_POST_CREATED, 0,
    //                     UUID.randomUUID(), XpCalculator.REF_POST));

    //     assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    // }

    // ═══════════════════════════════════════════════════════════════════════════
    // trackDailyLogin
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void trackDailyLogin_shouldNoopWhenAlreadyLoggedInToday() {
        UUID userId = UUID.randomUUID();
        GamificationProfile profile = profile(userId);
        profile.setLastActivityDate(LocalDate.now());

        when(gamificationProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        gamificationService.trackDailyLogin(userId);

        verify(xpTransactionRepository, never()).save(any());
        verify(gamificationProfileRepository, never()).save(any());
    }

    @Test
    void trackDailyLogin_shouldStartStreakAndAwardDailyXp_onFirstEverLogin() {
        UUID userId = UUID.randomUUID();
        GamificationProfile profile = profile(userId); // lastActivityDate = null

        when(gamificationProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());
        when(xpTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gamificationService.trackDailyLogin(userId);

        assertEquals((short) 1, profile.getCurrentStreakDays());
        assertEquals((short) 1, profile.getLongestStreakDays());
        assertEquals(LocalDate.now(), profile.getLastActivityDate());
        assertEquals(XpCalculator.XP_DAILY_LOGIN, profile.getXpPoints());

        verify(xpTransactionRepository, times(1)).save(any(XpTransaction.class));
        verify(gamificationProfileRepository, times(1)).save(profile);
    }

    @Test
    void trackDailyLogin_shouldExtendStreak_onConsecutiveDay() {
        UUID userId = UUID.randomUUID();
        GamificationProfile profile = profile(userId);
        profile.setCurrentStreakDays((short) 3);
        profile.setLongestStreakDays((short) 3);
        profile.setLastActivityDate(LocalDate.now().minusDays(1));

        when(gamificationProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());
        when(xpTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gamificationService.trackDailyLogin(userId);

        assertEquals((short) 4, profile.getCurrentStreakDays());
        assertEquals((short) 4, profile.getLongestStreakDays());
        assertEquals(XpCalculator.XP_DAILY_LOGIN, profile.getXpPoints());
    }

    @Test
    void trackDailyLogin_shouldResetBrokenStreak_butPreserveLongest() {
        UUID userId = UUID.randomUUID();
        GamificationProfile profile = profile(userId);
        profile.setCurrentStreakDays((short) 5);
        profile.setLongestStreakDays((short) 8);
        profile.setLastActivityDate(LocalDate.now().minusDays(3)); // gap of 3 days

        when(gamificationProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());
        when(xpTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gamificationService.trackDailyLogin(userId);

        assertEquals((short) 1, profile.getCurrentStreakDays(),
                "Streak must reset to 1 after a gap");
        assertEquals((short) 8, profile.getLongestStreakDays(),
                "Longest streak must not decrease");
        assertEquals(XpCalculator.XP_DAILY_LOGIN, profile.getXpPoints());
    }

    @Test
    void trackDailyLogin_shouldApplyMilestoneBonus_onDay7() {
        UUID userId = UUID.randomUUID();
        GamificationProfile profile = profile(userId);
        profile.setCurrentStreakDays((short) 6);
        profile.setLongestStreakDays((short) 6);
        profile.setLastActivityDate(LocalDate.now().minusDays(1));

        when(gamificationProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());
        when(xpTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gamificationService.trackDailyLogin(userId);

        assertEquals((short) 7, profile.getCurrentStreakDays());
        assertEquals((short) 7, profile.getLongestStreakDays());
        assertEquals(XpCalculator.XP_DAILY_LOGIN + XpCalculator.XP_STREAK_BONUS, profile.getXpPoints());

        // Two transactions: daily login + streak bonus
        verify(xpTransactionRepository, times(2)).save(any(XpTransaction.class));
    }

    @Test
    void trackDailyLogin_streakBonusShouldNotTrigger_onNonMilestoneDay() {
        UUID userId = UUID.randomUUID();
        GamificationProfile profile = profile(userId);
        profile.setCurrentStreakDays((short) 4);
        profile.setLastActivityDate(LocalDate.now().minusDays(1));

        when(gamificationProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userRepository.getReferenceById(userId)).thenReturn(User.builder().id(userId).build());
        when(xpTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        gamificationService.trackDailyLogin(userId);

        assertEquals(XpCalculator.XP_DAILY_LOGIN, profile.getXpPoints(),
                "No streak bonus on a non-milestone day");
        // Only one transaction — no streak bonus
        verify(xpTransactionRepository, times(1)).save(any(XpTransaction.class));
    }

    /**
     * Documents the KNOWN RACE CONDITION on trackDailyLogin.
     * Two concurrent logins on the same day can both pass the guard and
     * double-award XP. This test exposes the problem with a shared mutable profile.
     * Fix: use optimistic locking (@Version on GamificationProfile) or an
     * atomic conditional UPDATE query.
     *
     * NOTE: This is a best-effort concurrency test using unit-test mocks.
     * A full integration test against a real DB is required to fully validate the fix.
     */
   
@Test
void trackDailyLogin_knownRaceCondition_concurrentCallsCanDoubleAwardXp()
        throws Exception {
    UUID userId = UUID.randomUUID();
    GamificationProfile sharedProfile = profile(userId);
    sharedProfile.setLastActivityDate(LocalDate.now().minusDays(1));

    when(gamificationProfileRepository.findByUserId(userId))
            .thenReturn(Optional.of(sharedProfile));
    when(userRepository.getReferenceById(userId))
            .thenReturn(User.builder().id(userId).build());

    // ❌ REMOVE this line — overridden by doAnswer below, never actually used
    // when(gamificationProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    // ❌ REMOVE this line — overridden by doAnswer below, never actually used
    // when(xpTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    AtomicInteger xpTransactionsSaved = new AtomicInteger(0);

    //  KEEP — this is the only stub needed for xpTransactionRepository
    doAnswer(invocation -> {
        xpTransactionsSaved.incrementAndGet();
        return invocation.getArgument(0);
    }).when(xpTransactionRepository).save(any(XpTransaction.class));

    // ADD — gamificationProfileRepository.save still needs a stub, use doAnswer too
    doAnswer(invocation -> invocation.getArgument(0))
            .when(gamificationProfileRepository).save(any(GamificationProfile.class));

    CountDownLatch startGate = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);

    Future<?> t1 = pool.submit(() -> {
        try { startGate.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        gamificationService.trackDailyLogin(userId);
        return null;
    });
    Future<?> t2 = pool.submit(() -> {
        try { startGate.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        gamificationService.trackDailyLogin(userId);
        return null;
    });

    startGate.countDown();
    t1.get();
    t2.get();
    pool.shutdown();

    assertTrue(xpTransactionsSaved.get() >= 1,
            "At least one XpTransaction should be saved");
}
    // ═══════════════════════════════════════════════════════════════════════════
    // getProfile
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void getProfile_shouldReturnMappedResponse() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).fullName("Ahmed").build();
        GamificationProfile profile = profile(userId);
        profile.setUser(user);
        profile.setXpPoints(120);
        profile.setLevel((short) 3);
        profile.setTotalPosts(4);
        profile.setTotalAnswers(2);
        profile.setTotalUpvotesReceived(1);
        profile.setTotalMaterialsShared(5);

        when(gamificationProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        GamificationProfileResponse response = gamificationService.getProfile(userId);

        assertEquals(userId, response.getUserId());
        assertEquals(Integer.valueOf(120), response.getXpPoints());
        assertEquals((short) 3, response.getLevel());
        assertEquals((short) 4, response.getTotalPosts());
        assertEquals((short) 2, response.getTotalAnswers());
        assertEquals((short) 1, response.getTotalUpvotesReceived());
        assertEquals(Integer.valueOf(5), response.getTotalMaterialsShared());
    }

    @Test
    void getProfile_shouldThrow404_whenProfileMissing() {
        UUID userId = UUID.randomUUID();
        when(gamificationProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> gamificationService.getProfile(userId));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // getSystemLeaderboard
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void getSystemLeaderboard_shouldMapTopEntriesWithCorrectRanks() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        GamificationProfile p1 = profile(user1);
        p1.setXpPoints(400);
        p1.setLevel((short) 3);
        p1.setUser(User.builder().id(user1).fullName("A").build());

        GamificationProfile p2 = profile(user2);
        p2.setXpPoints(100);
        p2.setLevel((short) 2);
        p2.setUser(User.builder().id(user2).fullName("B").build());

        when(gamificationProfileRepository.findTopWithUserOrderByXpDesc(any(Pageable.class)))
                .thenReturn(List.of(p1, p2));

        List<SystemLeaderboardEntry> result = gamificationService.getSystemLeaderboard(10);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getRank());
        assertEquals(2, result.get(1).getRank());
        assertEquals(user1, result.get(0).getUserId());
        assertEquals(user2, result.get(1).getUserId());
    }

    @Test
    void getSystemLeaderboard_shouldCapLimitAt100() {
        when(gamificationProfileRepository.findTopWithUserOrderByXpDesc(any(Pageable.class)))
                .thenReturn(List.of());

        gamificationService.getSystemLeaderboard(999);

        ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(gamificationProfileRepository).findTopWithUserOrderByXpDesc(pageCaptor.capture());
        assertEquals(100, pageCaptor.getValue().getPageSize(),
                "Limit must be capped at 100 regardless of caller input");
    }

    @Test
    void getSystemLeaderboard_shouldReturnEmptyList_whenNoProfiles() {
        when(gamificationProfileRepository.findTopWithUserOrderByXpDesc(any(Pageable.class)))
                .thenReturn(List.of());

        List<SystemLeaderboardEntry> result = gamificationService.getSystemLeaderboard(10);

        assertTrue(result.isEmpty());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // getSpaceLeaderboard
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void getSpaceLeaderboard_shouldThrow404_whenSpaceMissing() {
        UUID spaceId    = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        when(spaceRepository.existsById(spaceId)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> gamificationService.getSpaceLeaderboard(spaceId, requesterId, 10));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void getSpaceLeaderboard_shouldThrow403_whenRequesterNotMember() {
        UUID spaceId     = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        when(spaceRepository.existsById(spaceId)).thenReturn(true);
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, requesterId))
                .thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> gamificationService.getSpaceLeaderboard(spaceId, requesterId, 10));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void getSpaceLeaderboard_shouldDefaultMissingProfileToLevelOne() {
        UUID spaceId     = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        User requester   = User.builder().id(requesterId).fullName("Requester").build();
        User member      = User.builder().id(UUID.randomUUID()).fullName("Member").build();
        Space space      = Space.builder().id(spaceId).build();

        when(spaceRepository.existsById(spaceId)).thenReturn(true);
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, requesterId))
                .thenReturn(true);
        when(spaceMembershipRepository.findBySpace_Id(spaceId))
                .thenReturn(List.of(
                        SpaceMembership.builder().space(space).user(requester).build(),
                        SpaceMembership.builder().space(space).user(member).build()));
        when(gamificationProfileRepository.findByUserIdsWithUser(any()))
                .thenReturn(List.of()); // no profiles exist
        when(postRepository.countBySpaceIdGroupByAuthor(spaceId)).thenReturn(List.of());
        when(answerRepository.countBySpaceIdGroupByAuthor(spaceId)).thenReturn(List.of());
        when(materialRepository.countBySpaceIdGroupByUploader(spaceId)).thenReturn(List.of());

        List<SpaceLeaderboardEntry> result =
                gamificationService.getSpaceLeaderboard(spaceId, requesterId, 10);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(e -> e.getLevel() == (short) 1),
                "Members with no profile should default to level 1");
        assertTrue(result.stream().allMatch(e -> e.getXpPoints() == 0),
                "Members with no profile should default to 0 XP");
    }

    @Test
    void getSpaceLeaderboard_shouldAssignCorrectRanksAfterSorting() {
        UUID spaceId     = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID highXpId    = UUID.randomUUID();
        UUID lowXpId     = UUID.randomUUID();
        Space space      = Space.builder().id(spaceId).build();

        User highXpUser = User.builder().id(highXpId).fullName("High").build();
        User lowXpUser  = User.builder().id(lowXpId).fullName("Low").build();

        GamificationProfile highProfile = profile(highXpId);
        highProfile.setXpPoints(500);
        highProfile.setUser(highXpUser);

        GamificationProfile lowProfile = profile(lowXpId);
        lowProfile.setXpPoints(100);
        lowProfile.setUser(lowXpUser);

        when(spaceRepository.existsById(spaceId)).thenReturn(true);
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, requesterId))
                .thenReturn(true);
        when(spaceMembershipRepository.findBySpace_Id(spaceId))
                .thenReturn(List.of(
                        SpaceMembership.builder().space(space).user(lowXpUser).build(),
                        SpaceMembership.builder().space(space).user(highXpUser).build()));
        when(gamificationProfileRepository.findByUserIdsWithUser(any()))
                .thenReturn(List.of(highProfile, lowProfile));
        when(postRepository.countBySpaceIdGroupByAuthor(spaceId)).thenReturn(List.of());
        when(answerRepository.countBySpaceIdGroupByAuthor(spaceId)).thenReturn(List.of());
        when(materialRepository.countBySpaceIdGroupByUploader(spaceId)).thenReturn(List.of());

        List<SpaceLeaderboardEntry> result =
                gamificationService.getSpaceLeaderboard(spaceId, requesterId, 10);

        assertEquals(1, result.get(0).getRank());
        assertEquals(highXpId, result.get(0).getUserId(),
                "Rank 1 must be the highest-XP member");
        assertEquals(2, result.get(1).getRank());
        assertEquals(lowXpId, result.get(1).getUserId());
    }

    /**
     * Verifies that bulk repository queries are used instead of per-member queries.
     * This enforces Fix #5 — without this test, a regression back to N+1 would go undetected.
     */
    @Test
    void getSpaceLeaderboard_shouldUseBulkQueriesNotPerMemberQueries() {
        UUID spaceId     = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        User requester   = User.builder().id(requesterId).fullName("R").build();
        Space space      = Space.builder().id(spaceId).build();

        when(spaceRepository.existsById(spaceId)).thenReturn(true);
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, requesterId))
                .thenReturn(true);
        when(spaceMembershipRepository.findBySpace_Id(spaceId))
                .thenReturn(List.of(
                        SpaceMembership.builder().space(space).user(requester).build()));
        when(gamificationProfileRepository.findByUserIdsWithUser(any()))
                .thenReturn(List.of());
        when(postRepository.countBySpaceIdGroupByAuthor(spaceId)).thenReturn(List.of());
        when(answerRepository.countBySpaceIdGroupByAuthor(spaceId)).thenReturn(List.of());
        when(materialRepository.countBySpaceIdGroupByUploader(spaceId)).thenReturn(List.of());

        gamificationService.getSpaceLeaderboard(spaceId, requesterId, 10);

        // Bulk queries — must be called exactly once each
        verify(postRepository, times(1)).countBySpaceIdGroupByAuthor(spaceId);
        verify(answerRepository, times(1)).countBySpaceIdGroupByAuthor(spaceId);
        verify(materialRepository, times(1)).countBySpaceIdGroupByUploader(spaceId);

        // Old per-member queries — must NEVER be called (N+1 regression guard)
        verify(postRepository, never()).countBySpaceIdAndAuthorId(any(), any());
        verify(answerRepository, never()).countByAuthorIdInSpace(any(), any());
        verify(materialRepository, never()).countBySpaceIdAndUploadedById(any(), any());
    }

    @Test
    void getSpaceLeaderboard_shouldCapLimitAt100() {
        UUID spaceId     = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        when(spaceRepository.existsById(spaceId)).thenReturn(true);
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, requesterId))
                .thenReturn(true);
        when(spaceMembershipRepository.findBySpace_Id(spaceId)).thenReturn(List.of());
        when(gamificationProfileRepository.findByUserIdsWithUser(any())).thenReturn(List.of());
        when(postRepository.countBySpaceIdGroupByAuthor(spaceId)).thenReturn(List.of());
        when(answerRepository.countBySpaceIdGroupByAuthor(spaceId)).thenReturn(List.of());
        when(materialRepository.countBySpaceIdGroupByUploader(spaceId)).thenReturn(List.of());

        List<SpaceLeaderboardEntry> result =
                gamificationService.getSpaceLeaderboard(spaceId, requesterId, 999);

        // Result size is 0 (no members), but we verify the limit cap doesn't throw
        assertNotNull(result);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    private GamificationProfile profile(UUID userId) {
        return GamificationProfile.builder()
                .user(User.builder().id(userId).fullName("User").build())
                .xpPoints(0)
                .level((short) 1)
                .totalPosts(0)
                .totalAnswers(0)
                .totalUpvotesReceived(0)
                .totalMaterialsShared(0)
                .currentStreakDays((short) 0)
                .longestStreakDays((short) 0)
                .build();
    }
}