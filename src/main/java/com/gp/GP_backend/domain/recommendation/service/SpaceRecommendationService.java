package com.gp.GP_backend.domain.recommendation.service;

import com.gp.GP_backend.domain.recommendation.client.RecommendationClient;
import com.gp.GP_backend.domain.recommendation.dto.SpaceRankingResponse;
import com.gp.GP_backend.domain.recommendation.dto.SpaceRecommendationRankRequest;
import com.gp.GP_backend.domain.recommendation.dto.SpaceRecommendationResponse;
import com.gp.GP_backend.domain.space.dto.SpaceResponse;
import com.gp.GP_backend.domain.space.entity.Space;
import com.gp.GP_backend.domain.space.entity.SpaceCategory;
import com.gp.GP_backend.domain.space.entity.SpaceMembership;
import com.gp.GP_backend.domain.space.repository.SpaceMembershipRepository;
import com.gp.GP_backend.domain.space.repository.SpaceRepository;
import com.gp.GP_backend.domain.course.entity.CourseRegistration;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.course.repository.CourseRegistrationRepository;
import com.gp.GP_backend.domain.user.repository.UserRepository;
import com.gp.GP_backend.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpaceRecommendationService {

    private static final int MAX_COURSE_CANDIDATES = 100;
    private static final int MAX_SOCIAL_CANDIDATES = 100;
    private static final int MAX_POPULAR_CANDIDATES = 50;
    private static final int MAX_REQUEST_CANDIDATES = 300;

    private final UserRepository userRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;
    private final SpaceMembershipRepository membershipRepository;
    private final SpaceRepository spaceRepository;
    private final RecommendationClient recommendationClient;

    @Transactional(readOnly = true)
    public List<SpaceRecommendationResponse> recommend(UUID userId, int topN) {
        // Clamp topN to a safe range: minimum 1, maximum 50
        int safeTopN = Math.max(1, Math.min(topN, 50));

        // Load user from database; throw 404 if not found
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found with id: " + userId));

        // Step 1: Gather user context
        // Get current course codes for course-match candidate generation
        List<String> currentCourses = getCurrentCourseCodes(userId);
        // Load all spaces the user is already a member of (to exclude from recommendations)
        List<SpaceMembership> memberships = membershipRepository.findByUserIdWithSpace(userId);

        // Create a snapshot of joined spaces to send to the Python ranking service
        List<JoinedSpaceSnapshot> joinedSpaces = memberships.stream()
                .map(membership -> JoinedSpaceSnapshot.from(membership.getSpace()))
                .toList();

        // Step 2: Candidate Generation (Java-side)
        // Use a LinkedHashMap to preserve insertion order and deduplicate spaces by ID
        Map<UUID, CandidateSpace> candidateMap = new LinkedHashMap<>();
        // Generate candidates from user's current courses
        collectCourseCandidates(userId, candidateMap, currentCourses);
        // Generate candidates from "friends of friends" social graph
        collectSocialCandidates(userId, candidateMap);
        // Generate popular/trending candidates as fallback
        collectPopularCandidates(userId, candidateMap);

        // Step 3: Candidate Selection and Prioritization
        // Sort candidates by relevance heuristics (multi-source hits, member count, recency, name)
        // Cap the candidate list to MAX_REQUEST_CANDIDATES before sending to Python for ranking
        List<CandidateSpace> selectedCandidates = candidateMap.values().stream()
                .sorted(candidateComparator())
                .limit(MAX_REQUEST_CANDIDATES)
                .toList();

        // Bail early if no candidates are available
        if (selectedCandidates.isEmpty()) {
            return List.of();
        }

        // Step 4: Build request payload for Python ranking service
        // Include user context (ID, courses, already-joined spaces) and bounded candidate list
        SpaceRecommendationRankRequest request = SpaceRecommendationRankRequest.builder()
                .user(SpaceRecommendationRankRequest.UserContext.builder()
                        .id(user.getId())
                        .courses(currentCourses)
                        .joinedSpaces(joinedSpaces.stream()
                                .map(JoinedSpaceSnapshot::toRequest)
                                .toList())
                        .build())
                .candidateSpaces(selectedCandidates.stream()
                        .map(CandidateSpace::toRequest)
                        .toList())
                .build();

        // Step 5: Call Python ranking service (stateless, reads-only, no DB access)
        // Python receives the candidates and user context, returns ranked space IDs with scores
        List<SpaceRankingResponse> rankedResults = recommendationClient.rankSpaces(request);
        // Bail early if Python returned no results
        if (rankedResults.isEmpty()) {
            return List.of();
        }

        // Step 6: Build lookup maps for efficient merging
        // Map space IDs to candidate metadata (sources, etc.)
        Map<UUID, CandidateSpace> candidateLookup = selectedCandidates.stream()
                .collect(Collectors.toMap(CandidateSpace::spaceId, candidate -> candidate));

        // Extract ranked space IDs in order from Python response, filtering out any unknowns
        List<UUID> rankedIds = rankedResults.stream()
                .map(SpaceRankingResponse::getSpaceId)
                .filter(candidateLookup::containsKey)
                .toList();

        // Bail early if no ranked results pass the filtering step
        if (rankedIds.isEmpty()) {
            return List.of();
        }

        // Map space IDs to scores from Python response (used to include ranking score in response)
        Map<UUID, Double> scoreLookup = rankedResults.stream()
                .filter(result -> result.getSpaceId() != null && result.getScore() != null)
                .collect(Collectors.toMap(SpaceRankingResponse::getSpaceId, SpaceRankingResponse::getScore,
                        (first, second) -> first));

        // Fetch full Space entities from DB using eager-load query to include creator information
        Map<UUID, Space> spaceLookup = spaceRepository.findAllByIdWithCreator(rankedIds).stream()
                .collect(Collectors.toMap(Space::getId, space -> space));

        // Step 7: Merge Python rankings with Space data fetched from DB
        // Iterate through ranked IDs to preserve Python's ranking order during merge
        List<SpaceRecommendationResponse> responses = new ArrayList<>();
        for (UUID spaceId : rankedIds) {
            // Safely retrieve candidate, space, and score; skip if any piece is missing
            CandidateSpace candidate = candidateLookup.get(spaceId);
            Space space = spaceLookup.get(spaceId);
            Double score = scoreLookup.get(spaceId);
            if (candidate == null || space == null || score == null) {
                continue;
            }
            // Build response DTO with Python score and recommendation source metadata
            responses.add(toResponse(space, score, candidate.sources));
            // Stop early once we have enough results
            if (responses.size() == safeTopN) {
                break;
            }
        }

        // Step 8: Sort final responses by score (descending) to ensure consistent ordering
        // Secondary sort by method count (multi-source hits ranked higher)
        // Tertiary sort by space name (deterministic tie-breaker)
        Comparator<SpaceRecommendationResponse> rankingComparator = Comparator
                .comparingDouble(SpaceRecommendationResponse::getScore).reversed()
                .thenComparing(Comparator.comparingInt(SpaceRecommendationResponse::getMethodCount).reversed())
                .thenComparing((left, right) -> compareSpaces(left.getSpace(), right.getSpace()));

        return responses.stream()
                .sorted(rankingComparator)
                .toList();
    }

    /**
     * Collect candidate spaces based on the user's current course enrollments.
     * Finds spaces associated with the user's courses that they haven't already joined.
     */
    private void collectCourseCandidates(UUID userId, Map<UUID, CandidateSpace> out, List<String> courseCodes) {
        // Normalize course codes: trim whitespace, convert to uppercase, remove duplicates and blanks
        List<String> normalized = courseCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .map(code -> code.toUpperCase(Locale.ROOT))
                .distinct()
                .toList();

        // Skip course candidate collection if user has no current courses
        if (normalized.isEmpty()) {
            return;
        }

        // Query DB for college course spaces matching user's enrollments (excluding already-joined)
        // Cap results to MAX_COURSE_CANDIDATES to avoid overwhelming the candidate pool
        spaceRepository.findCollegeCourseSpacesNotJoined(normalized, userId, SpaceCategory.COLLEGE_COURSE).stream()
                .limit(MAX_COURSE_CANDIDATES)
                .forEach(space -> candidate(out, space).addSource(RecommendationSource.COURSE_MATCH));
    }

    /**
     * Collect candidate spaces based on social graph:
     * Finds spaces that are popular among users similar to the current user.
     * Implements "friends of friends" recommendation: if many of your peers are in a space, it's likely relevant.
     */
    private void collectSocialCandidates(UUID userId, Map<UUID, CandidateSpace> out) {
        // Query DB for "friends of friends" space scores (spaces popular among similar users)
        List<Object[]> rows = membershipRepository.findFofSpaceScores(userId);
        if (rows.isEmpty()) {
            return;
        }

        // Cap social candidates to MAX_SOCIAL_CANDIDATES
        List<Object[]> limitedRows = rows.stream()
                .limit(MAX_SOCIAL_CANDIDATES)
                .toList();

        // Extract space IDs from the query results (each row[0] is a space ID)
        List<UUID> spaceIds = limitedRows.stream()
                .map(row -> toUuid(row[0]))
                .toList();

        // Fetch full Space entities from DB (eager-load creator info for response building)
        Map<UUID, Space> spaceLookup = spaceRepository.findAllByIdWithCreator(spaceIds).stream()
                .collect(Collectors.toMap(Space::getId, space -> space));

        // Map each space ID to its CandidateSpace, adding SOCIAL source tag
        for (Object[] row : limitedRows) {
            UUID spaceId = toUuid(row[0]);
            Space space = spaceLookup.get(spaceId);
            if (space == null) {
                continue;
            }
            candidate(out, space).addSource(RecommendationSource.SOCIAL);
        }
    }

    /**
     * Collect candidate spaces based on popularity and recency.
     * Serves as a fallback recommendation source: trending/active spaces are likely valuable.
     * Uses pagination to fetch top MAX_POPULAR_CANDIDATES sorted by member count (descending) and creation date.
     */
    private void collectPopularCandidates(UUID userId, Map<UUID, CandidateSpace> out) {
        // Query DB for popular spaces the user hasn't joined
        // Sort by memberCount descending (most popular first), then by createdAt (newer spaces first as tie-breaker)
        // Use pagination to efficiently fetch exactly MAX_POPULAR_CANDIDATES
        spaceRepository.findPopularSpacesNotJoined(userId,
                        PageRequest.of(0, MAX_POPULAR_CANDIDATES,
                                Sort.by(Sort.Direction.DESC, "memberCount", "createdAt")))
                .getContent()
                // Add each popular space to the candidate pool with POPULAR source tag
                .forEach(space -> candidate(out, space).addSource(RecommendationSource.POPULAR));
    }

    private List<String> getCurrentCourseCodes(UUID userId) {
        return courseRegistrationRepository.findByUserIdAndClosedFalse(userId).stream()
                .map(CourseRegistration::getCode)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(code -> code.toUpperCase(Locale.ROOT))
                .filter(code -> !code.isBlank())
                .distinct()
                .toList();
    }

    private static CandidateSpace candidate(Map<UUID, CandidateSpace> map, Space space) {
        return map.computeIfAbsent(space.getId(), id -> new CandidateSpace(space));
    }

    /**
     * Defines the sorting order for candidates before sending to Python.
     * Prioritizes by: multi-source hits > member count > recency > name (for determinism).
     */
    private static Comparator<CandidateSpace> candidateComparator() {
        // Primary: spaces matching multiple recommendation sources rank higher
        return Comparator.comparingInt(CandidateSpace::sourceCount).reversed()
                // Secondary: spaces with more members are more established/valuable
                .thenComparing(Comparator.comparingInt(CandidateSpace::memberCount).reversed())
                // Tertiary: more recently active spaces rank higher
                .thenComparing(Comparator.comparing(CandidateSpace::lastActivityDate,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                // Quaternary: newer spaces rank higher (creation date)
                .thenComparing(Comparator.comparing(CandidateSpace::createdAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                // Quinary: space name (alphabetical) as final deterministic tie-breaker
                .thenComparing(CandidateSpace::spaceName, String.CASE_INSENSITIVE_ORDER);
    }

    private static UUID toUuid(Object raw) {
        return (raw instanceof UUID) ? (UUID) raw : UUID.fromString(raw.toString());
    }

    /**
     * Compares two spaces by name (case-insensitive) for deterministic tie-breaking.
     * Used in final response sorting when scores are equal.
     */
    private static int compareSpaces(SpaceResponse left, SpaceResponse right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        String leftName = left.getName() != null ? left.getName() : "";
        String rightName = right.getName() != null ? right.getName() : "";
        return String.CASE_INSENSITIVE_ORDER.compare(leftName, rightName);
    }

    /**
     * Builds a response DTO from a Space entity, Python ranking score, and recommendation sources.
     * Includes the score from Python and metadata about why this space was recommended.
     */
    private static SpaceRecommendationResponse toResponse(Space space, double score, Set<RecommendationSource> sources) {
        return SpaceRecommendationResponse.builder()
                // Convert Space entity to response DTO (includes creator info, category, etc.)
                .space(toSpaceResponse(space))
                // Ranking score from Python service (higher = more relevant to this user)
                .score(score)
                // Count of distinct recommendation sources (COURSE_MATCH, SOCIAL, POPULAR)
                .methodCount(sources.size())
                // List of recommendation source names for explanation/transparency to client
                .reasons(sources.stream().map(Enum::name).collect(Collectors.toCollection(java.util.LinkedHashSet::new)))
                .build();
    }

    /**
     * Converts a Space entity to a SpaceResponse DTO for API serialization.
     * Includes space metadata, creator info, and engagement metrics.
     */
    private static SpaceResponse toSpaceResponse(Space space) {
        return SpaceResponse.builder()
                // Space identifiers
                .id(space.getId())
                .name(space.getName())
                .slug(space.getSlug())
                // Space categorization and course link (if applicable)
                .description(space.getDescription())
                .category(space.getCategory() != null ? space.getCategory().name() : null)
                .courseCode(space.getCourseCode())
                // Creator/author information
                .createdById(space.getCreatedBy() != null ? space.getCreatedBy().getId() : null)
                .createdByName(space.getCreatedBy() != null ? space.getCreatedBy().getFullName() : null)
                // Space status and engagement metrics
                .isActive(space.getIsActive())
                .memberCount(space.getMemberCount())
                .createdAt(space.getCreatedAt())
                .build();
    }

    private enum RecommendationSource {
        COURSE_MATCH,
        SOCIAL,
        POPULAR
    }

    /**
     * Internal DTO: tracks a candidate space and which recommendation sources qualified it.
     * Used during candidate generation phase before sending to Python.
     */
    private static final class CandidateSpace {
        private final Space space;
        // EnumSet tracks which methods recommended this space (COURSE_MATCH, SOCIAL, POPULAR)
        private final EnumSet<RecommendationSource> sources = EnumSet.noneOf(RecommendationSource.class);

        private CandidateSpace(Space space) {
            this.space = space;
        }

        /**
         * Mark this candidate as recommended by a particular source.
         * Spaces matching multiple sources get higher priority during ranking.
         */
        private void addSource(RecommendationSource source) {
            sources.add(source);
        }

        // Accessor methods for comparator chain
        private UUID spaceId() {
            return space.getId();
        }

        // Space name for deterministic tie-breaking in candidate ordering
        private String spaceName() {
            return space.getName();
        }

        // Number of distinct recommendation sources (1-3 range)
        private int sourceCount() {
            return sources.size();
        }

        // Engagement metric: member count (null-safe)
        private int memberCount() {
            return Optional.ofNullable(space.getMemberCount()).orElse(0);
        }

        // Recency metric: date portion of creation timestamp
        private LocalDate lastActivityDate() {
            return space.getCreatedAt() == null ? null : space.getCreatedAt().toLocalDate();
        }

        // Exact creation timestamp for secondary sorting
        private java.time.LocalDateTime createdAt() {
            return space.getCreatedAt();
        }

        /**
         * Converts this candidate to a request DTO for the Python ranking service.
         * Includes space ID, title, description, and engagement metrics.
         */
        private SpaceRecommendationRankRequest.CandidateSpace toRequest() {
            return SpaceRecommendationRankRequest.CandidateSpace.builder()
                    .id(space.getId())
                    .title(space.getName())
                    .description(space.getDescription())
                    .memberCount(space.getMemberCount())
                    .lastActivityDate(lastActivityDate())
                    .build();
        }
    }

    /**
     * Internal DTO: snapshot of a space the user has already joined.
     * Sent to Python so the ranking service knows which spaces are "already consumed" by this user.
     * Used to avoid recommending spaces the user is already a member of.
     */
    private static final class JoinedSpaceSnapshot {
        private final UUID id;
        private final String title;
        private final String description;

        private JoinedSpaceSnapshot(UUID id, String title, String description) {
            this.id = id;
            this.title = title;
            this.description = description;
        }

        /**
         * Factory method: creates a snapshot from a Space entity.
         */
        private static JoinedSpaceSnapshot from(Space space) {
            return new JoinedSpaceSnapshot(space.getId(), space.getName(), space.getDescription());
        }

        /**
         * Converts this snapshot to a request DTO for the Python ranking service.
         */
        private SpaceRecommendationRankRequest.JoinedSpace toRequest() {
            return SpaceRecommendationRankRequest.JoinedSpace.builder()
                    .id(id)
                    .title(title)
                    .description(description)
                    .build();
        }
    }
}
