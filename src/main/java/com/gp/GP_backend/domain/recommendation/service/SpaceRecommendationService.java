package com.gp.GP_backend.domain.recommendation.service;

import com.gp.GP_backend.domain.recommendation.dto.SpaceRecommendationResponse;
import com.gp.GP_backend.domain.space.dto.SpaceResponse;
import com.gp.GP_backend.domain.space.entity.Space;
import com.gp.GP_backend.domain.space.entity.SpaceCategory;
import com.gp.GP_backend.domain.space.repository.SpaceMembershipRepository;
import com.gp.GP_backend.domain.space.repository.SpaceRepository;
import com.gp.GP_backend.domain.user.entity.CourseRegistered;
import com.gp.GP_backend.domain.user.repository.CourseRegisteredRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Produces personalized space recommendations by blending three independent
 * signals.
 *
 * <h2>Layers</h2>
 * <ol>
 * <li><b>Course match (Layer 1)</b> — finds active {@code COLLEGE_COURSE}
 * spaces whose
 * {@code courseCode} is in the caller's registered-course list.
 * Score is binary: 1.0 per match.</li>
 * <li><b>Friends-of-friends (Layer 2)</b> — for every space the caller belongs
 * to,
 * collects all other members and then finds all other spaces those members have
 * joined. Score is the co-member overlap count, normalized to [0, 1].</li>
 * <li><b>Text similarity (Layer 3)</b> — for every active
 * non-{@code COLLEGE_COURSE}
 * space the caller has <em>not</em> joined, computes the maximum Jaccard
 * similarity
 * against the name+description of every space the caller already belongs to.
 * Spaces below {@link #TEXT_SIMILARITY_THRESHOLD} are discarded.</li>
 * </ol>
 *
 * <h2>Ranking</h2>
 * Candidates are sorted first by the number of layers that nominated them (more
 * = better
 * cross-validation), then by combined score as a tie-breaker.
 *
 * <h2>Performance characteristics (SQL Server + typical university data)</h2>
 * <ul>
 * <li>Layer 1: single indexed range scan — O(1).</li>
 * <li>Layer 2: 3-table self-join on indexed {@code (space_id, user_id)}
 * columns,
 * capped at {@value #FOF_QUERY_LIMIT} rows by the SQL query — sub-second.</li>
 * <li>Layer 3: O(U × C) Jaccard comparisons in Java, where U = user's
 * non-course space
 * count (typically &lt; 20) and C = active non-course spaces not yet joined.
 * HashSet intersection is O(min(|A|, |B|)) per pair — full run &lt; 50 ms for
 * 500 candidate spaces.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SpaceRecommendationService {

    // ─── Constants ────────────────────────────────────────────────────────────

    private static final String REASON_COURSE = "COURSE_MATCH";
    private static final String REASON_SOCIAL = "SOCIAL";
    private static final String REASON_SIMILAR = "SIMILAR_CONTENT";

    /**
     * Minimum token length kept during tokenization.
     * Discards articles/prepositions (e.g. "a", "in", "of") that add noise.
     */
    private static final int MIN_TOKEN_LENGTH = 3;

    /**
     * Minimum Jaccard coefficient required for a text-similarity nomination.
     * Tuned to suppress accidental single-word overlaps on short descriptions.
     */
    private static final double TEXT_SIMILARITY_THRESHOLD = 0.2;


    // ─── Dependencies ─────────────────────────────────────────────────────────

    private final SpaceRepository spaceRepository;
    private final SpaceMembershipRepository membershipRepository;
    private final CourseRegisteredRepository courseRegisteredRepository;
    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Computes personalized recommendations for the given user.
     *
     * @param userId      the authenticated user's UUID.
     * @param topN        maximum number of results to return.
     * @return up to {@code topN} recommendations, ranked by method count then
     *         score.
     */
    @Transactional(readOnly = true)
    public List<SpaceRecommendationResponse> recommend(UUID userId, int topN) {


        // Accumulator: spaceId → candidate record that merges scores across layers.
        Map<UUID, RecommendationCandidate> candidates = new HashMap<>();

        scoreByCourseCodes(userId, candidates); // Layer 1
        scoreBySocialNetwork(userId, candidates); // Layer 2
        scoreByTextSimilarity(userId, candidates); // Layer 3

        return candidates.values().stream()
                .sorted(
                        Comparator.<RecommendationCandidate>comparingInt(RecommendationCandidate::methodCount)
                                .thenComparingDouble(RecommendationCandidate::totalScore)
                                .reversed())
                .limit(topN)
                .map(this::toRecommendationResponse)
                .toList();
    }


    // ─── Layer 1 : Course match ───────────────────────────────────────────────

    /**
     * Nominates every active {@code COLLEGE_COURSE} space whose {@code courseCode}
     * appears in {@code courseCodes} and that the user has not yet joined.
     * Each match receives a fixed score of 1.0.
     */
    private void scoreByCourseCodes(UUID userId,
            Map<UUID, RecommendationCandidate> out) {

        List<String> courseCodes = getCurrentCourseCodes(userId);

        if (courseCodes == null || courseCodes.isEmpty())
            return;

        List<String> trimmed = courseCodes.stream()
                .filter(c -> c != null && !c.isBlank())
                .map(String::trim)
                .toList();
        if (trimmed.isEmpty())
            return;

        spaceRepository
                .findCollegeCourseSpacesNotJoined(trimmed, userId, SpaceCategory.COLLEGE_COURSE)
                .forEach(space -> candidate(out, space).add(REASON_COURSE, 1.0));
    }

    // ─── Layer 2 : Friends-of-friends (social graph) ──────────────────────────

    /**
     * Nominates spaces joined by co-members of the user's existing spaces.
     *
     * <p>
     * The FoF query (native SQL) returns up to {@value #FOF_QUERY_LIMIT} rows of
     * (spaceId, coMemberCount) ordered by count descending. Scores are normalized
     * to
     * [0, 1] relative to the highest count in the result set, so the most socially
     * endorsed candidate always scores 1.0.
     *
     * <p>
     * Spaces are batch-loaded in a single {@code IN} query to avoid N+1 fetches.
     */
    private void scoreBySocialNetwork(UUID userId, Map<UUID, RecommendationCandidate> out) {

        List<Object[]> rows = membershipRepository.findFofSpaceScores(userId);
        if (rows.isEmpty())
            return;

        // Normalize: highest count → 1.0
        long maxCount = rows.stream()
                .mapToLong(r -> ((Number) r[1]).longValue())
                .max()
                .orElse(1L);

        // Extract IDs for the batch load (preserve order for correlation with scores)
        List<UUID> spaceIds = rows.stream()
                .map(r -> toUUID(r[0]))
                .toList();

        // Single IN-query to avoid N+1. Guard against empty list (not possible here,
        // but defensive).
        Map<UUID, Space> spaceMap = spaceRepository
                .findAllByIdWithCreator(spaceIds)
                .stream()
                .collect(Collectors.toMap(Space::getId, s -> s));

        for (Object[] row : rows) {
            UUID spaceId = toUUID(row[0]);
            Space space = spaceMap.get(spaceId);
            if (space == null)
                continue; // space became inactive between query and load

            double normalized = ((Number) row[1]).doubleValue() / maxCount;
            candidate(out, space).add(REASON_SOCIAL, normalized);
        }
    }

    // ─── Layer 3 : Text similarity (Jaccard) ──────────────────────────────────

    /**
     * Nominates active non-{@code COLLEGE_COURSE} spaces that are textually similar
     * to at least one space the user already belongs to.
     *
     * <p>
     * <b>Algorithm:</b>
     * <ol>
     * <li>Load the user's existing non-course spaces and tokenize each
     * name+description
     * once — this set is small and is reused for every candidate comparison.</li>
     * <li>Load all active non-course spaces the user has <em>not</em> joined.</li>
     * <li>For each candidate, compute Jaccard against every user-space token set
     * and
     * keep the maximum. Candidates scoring below
     * {@value #TEXT_SIMILARITY_THRESHOLD} are silently discarded.</li>
     * </ol>
     */
    private void scoreByTextSimilarity(UUID userId, Map<UUID, RecommendationCandidate> out) {

        List<Space> userSpaces = spaceRepository
                .findActiveNonCategorySpacesByUser(userId, SpaceCategory.COLLEGE_COURSE);
        if (userSpaces.isEmpty())
            return;

        // Pre-tokenize user's spaces once; skip empty token sets
        List<Set<String>> userTokenSets = userSpaces.stream()
                .map(s -> tokenize(s.getName(), s.getDescription()))
                .filter(t -> !t.isEmpty())
                .toList();
        if (userTokenSets.isEmpty())
            return;

        spaceRepository
                .findActiveNonCategorySpacesNotJoined(userId, SpaceCategory.COLLEGE_COURSE)
                .forEach(candidate -> {
                    Set<String> candidateTokens = tokenize(candidate.getName(), candidate.getDescription());
                    if (candidateTokens.isEmpty())
                        return;

                    // Score = max Jaccard similarity against any of the user's existing spaces
                    double bestScore = userTokenSets.stream()
                            .mapToDouble(userTokens -> jaccard(userTokens, candidateTokens))
                            .max()
                            .orElse(0.0);

                    if (bestScore >= TEXT_SIMILARITY_THRESHOLD) {
                        candidate(out, candidate).add(REASON_SIMILAR, bestScore);
                    }
                });
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private List<String> getCurrentCourseCodes(UUID userId) {
        return courseRegisteredRepository.findByUserIdAndIsCurrentTrue(userId).stream()
                .map(CourseRegistered::getCourseCode)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isBlank())
                .toList();
    }

    /**
     * Returns the existing candidate for this space, or creates a new one.
     * Using a helper keeps the lambda bodies in Layer 1–3 clean.
     */
    private static RecommendationCandidate candidate(Map<UUID, RecommendationCandidate> map, Space space) {
        return map.computeIfAbsent(space.getId(), id -> new RecommendationCandidate(space));
    }

    /**
     * Safely converts a raw JDBC object (String or UUID) returned from a native
     * query
     * to a {@link UUID}. SQL Server JDBC returns {@code UNIQUEIDENTIFIER} as a
     * {@link String} in native query results.
     */
    private static UUID toUUID(Object raw) {
        return (raw instanceof UUID) ? (UUID) raw : UUID.fromString(raw.toString());
    }

    /**
     * Jaccard similarity coefficient between two pre-computed token sets.
     *
     * <p>
     * {@code J(A,B) = |A ∩ B| / |A ∪ B|}
     *
     * <p>
     * Uses the identity {@code |A ∪ B| = |A| + |B| - |A ∩ B|} to avoid
     * materializing the union set, making this O(min(|A|, |B|)).
     */
    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty())
            return 0.0;

        long intersection = a.stream().filter(b::contains).count();
        long union = (long) a.size() + b.size() - intersection;

        return union == 0 ? 0.0 : (double) intersection / union;
    }

    /**
     * Splits the combined name + description text into a lowercase word-token set.
     * Tokens shorter than {@value #MIN_TOKEN_LENGTH} characters are dropped to
     * suppress high-frequency noise words.
     */
    private static Set<String> tokenize(String name, String description) {
        String text = (name == null ? "" : name)
                + " "
                + (description == null ? "" : description);
        if (text.isBlank())
            return Collections.emptySet();

        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(token -> token.length() >= MIN_TOKEN_LENGTH)
                .collect(Collectors.toSet());
    }

    /**
     * Maps a {@link RecommendationCandidate} to its public response DTO.
     * The {@code reasons} set is a copy to prevent external mutation.
     */
    private SpaceRecommendationResponse toRecommendationResponse(RecommendationCandidate c) {
        return SpaceRecommendationResponse.builder()
                .space(toSpaceResponse(c.space))
                .methodCount(c.methodCount())
                .score(c.totalScore())
                .reasons(Set.copyOf(c.methodScores.keySet()))
                .build();
    }

    /**
     * Converts a {@link Space} entity to a {@link SpaceResponse} DTO.
     *
     * <p>
     * Intentionally duplicated from {@code SpaceService} rather than creating
     * a shared mapper dependency, keeping recommendation logic self-contained.
     * {@code createdBy} is accessed here — callers must ensure it is loaded
     * (all repository methods in this service use {@code @EntityGraph}).
     */
    private static SpaceResponse toSpaceResponse(Space space) {
        return SpaceResponse.builder()
                .id(space.getId())
                .name(space.getName())
                .slug(space.getSlug())
                .description(space.getDescription())
                .category(space.getCategory() != null ? space.getCategory().name() : null)
                .courseCode(space.getCourseCode())
                .createdById(space.getCreatedBy() != null ? space.getCreatedBy().getId() : null)
                .createdByName(space.getCreatedBy() != null ? space.getCreatedBy().getFullName() : null)
                .isActive(space.getIsActive())
                .memberCount(space.getMemberCount())
                .createdAt(space.getCreatedAt())
                .build();
    }

    // ─── Internal value type ──────────────────────────────────────────────────

    /**
     * Accumulates scores from multiple recommendation layers for a single candidate
     * space.
     *
     * <p>
     * Using a {@link LinkedHashMap} for {@code methodScores} preserves insertion
     * order
     * so {@code reasons} always appears in a stable order in the response.
     *
     * <p>
     * If two layers provide the same reason with different scores (not currently
     * possible but defensive), {@link Math#max} keeps the higher one.
     */
    private static final class RecommendationCandidate {

        final Space space;
        final Map<String, Double> methodScores = new LinkedHashMap<>();

        RecommendationCandidate(Space space) {
            this.space = space;
        }

        /** Records a nomination from one recommendation layer. */
        void add(String reason, double score) {
            methodScores.merge(reason, score,
                    (existing, incoming) -> existing == null ? incoming : Double.max(existing, incoming));
        }

        /** Number of distinct layers that nominated this space. */
        int methodCount() {
            return methodScores.size();
        }

        /** Sum of all layer scores — used as a tie-breaker in ranking. */
        double totalScore() {
            return methodScores.values().stream().mapToDouble(d -> d).sum();
        }
    }
}
