package com.gp.GP_backend.domain.space.service;

import com.gp.GP_backend.domain.space.dto.CreateSpaceRequest;
import com.gp.GP_backend.domain.space.dto.MembershipResponse;
import com.gp.GP_backend.domain.space.dto.SpaceResponse;
import com.gp.GP_backend.domain.space.dto.UpdateSpaceRequest;
import com.gp.GP_backend.domain.space.entity.Space;
import com.gp.GP_backend.domain.space.entity.SpaceCategory;
import com.gp.GP_backend.domain.space.entity.SpaceMembership;
import com.gp.GP_backend.domain.space.repository.SpaceMembershipRepository;
import com.gp.GP_backend.domain.space.repository.SpaceRepository;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.exception.ApiException;
import com.gp.GP_backend.shared.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpaceService {

    /** Role string assigned to the space creator and promoted members. */
    private static final String ROLE_ADMIN = "ADMIN";

    /** Role string assigned to users who join a space voluntarily. */
    private static final String ROLE_MEMBER = "MEMBER";

    /** Valid sort fields for space search. */
    private static final Set<String> SPACE_SORT_FIELDS = Set.of("memberCount", "createdAt");

    private final SpaceRepository spaceRepository;
    private final SpaceMembershipRepository membershipRepository;

    // ─── Similarity check ─────────────────────────────────────────────────────

    /**
     * Runs the duplicate/similarity check for the given creation request.
     *
     * <p>
     * Returns a non-empty list when conflicts are found, or an empty list when
     * the space may be created safely. The controller returns HTTP 409 when the
     * list is non-empty and HTTP 201 when it is empty.
     *
     * @param request the incoming creation payload (not yet persisted).
     * @return sorted list of conflicting {@link SpaceResponse}s, or empty list.
     */
    @Transactional(readOnly = true)
    public List<SpaceResponse> checkSimilarity(CreateSpaceRequest request) {

        if (request.getCategory() == SpaceCategory.COLLEGE_COURSE) {
            return checkCollegeCourseConflict(request);
        }

        return checkTextSimilarityConflict(request);
    }

    /**
     * For {@link SpaceCategory#COLLEGE_COURSE}: returns all existing spaces that
     * share the same {@code courseCode}, sorted by {@code createdAt} descending.
     * Returns empty if {@code courseCode} is blank or no matches found.
     */
    private List<SpaceResponse> checkCollegeCourseConflict(CreateSpaceRequest request) {
        if (request.getCourseCode() == null || request.getCourseCode().isBlank()) {
            return Collections.emptyList();
        }

        return spaceRepository
                .findByCourseCodeOrderByCreatedAtDesc(request.getCourseCode())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * For all non-{@link SpaceCategory#COLLEGE_COURSE} categories: computes Jaccard
     * similarity between the candidate space and every active space in the same
     * category, then returns those with score &gt; 0 sorted by score descending.
     */
    private List<SpaceResponse> checkTextSimilarityConflict(CreateSpaceRequest request) {
        if (request.getCategory() == null) {
            return Collections.emptyList();
        }

        String candidateText = buildTextForSimilarity(request.getName(), request.getDescription());

        return spaceRepository
                .findByCategoryAndIsActiveTrue(request.getCategory())
                .stream()
                .map(existing -> {
                    String existingText = buildTextForSimilarity(existing.getName(), existing.getDescription());
                    double score = computeJaccardSimilarity(candidateText, existingText);
                    SpaceResponse resp = toResponse(existing);
                    resp.setSimilarityScore(score);
                    return resp;
                })
                .filter(r -> r.getSimilarityScore() > 0.0)
                .sorted(Comparator.comparingDouble(SpaceResponse::getSimilarityScore).reversed())
                .toList();
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    /**
     * Persists a new space and registers the creating user as its first
     * {@code ADMIN} member.
     *
     * <p>
     * This method is called after the similarity check either passes or is skipped
     * ({@code force=1}).
     *
     * @param request the validated creation payload.
     * @param creator the authenticated user who issued the request.
     * @return the persisted space as a response DTO.
     * @throws ApiException 409 if a space with the same name already exists.
     */
    @Transactional
    public SpaceResponse createSpace(CreateSpaceRequest request, User creator) {
        if (spaceRepository.existsByName(request.getName())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "A space named '" + request.getName() + "' already exists");
        }

        String slug = generateUniqueSlug(request.getName(), null);

        Space space = Space.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .category(request.getCategory())
                .courseCode(request.getCourseCode())
                .createdBy(creator)
                .isActive(true)
                .memberCount(1) // creator is the first member
                .build();

        space = spaceRepository.save(space);

        // Automatically enrol the creator as ADMIN
        SpaceMembership adminMembership = SpaceMembership.builder()
                .space(space)
                .user(creator)
                .role(ROLE_ADMIN)
                .build();
        membershipRepository.save(adminMembership);

        return toResponse(space);
    }

    // ─── Join ─────────────────────────────────────────────────────────────────

    /**
     * Adds the authenticated user to a space as a {@code MEMBER} and increments the
     * space's {@code memberCount}.
     *
     * @param spaceId the target space's UUID.
     * @param user    the authenticated user.
     * @return the newly created membership record.
     * @throws ApiException 404 if the space does not exist.
     * @throws ApiException 409 if the user is already a member.
     */
    @Transactional
    public MembershipResponse joinSpace(UUID spaceId, User user) {
        Space space = requireSpace(spaceId);

        if (membershipRepository.existsBySpaceIdAndUserId(spaceId, user.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "You are already a member of this space");
        }

        SpaceMembership membership = SpaceMembership.builder()
                .space(space)
                .user(user)
                .role(ROLE_MEMBER)
                .build();
        membership = membershipRepository.save(membership);

        space.setMemberCount(space.getMemberCount() + 1);
        spaceRepository.save(space);

        return toMembershipResponse(membership);
    }

    // ─── Leave ────────────────────────────────────────────────────────────────

    /**
     * Removes the authenticated user from a space and decrements
     * {@code memberCount}.
     *
     * <p>
     * <b>Guard:</b> if the user holds the {@code ADMIN} role and is the
     * <em>only</em>
     * admin remaining, the request is rejected with HTTP 400. The user must first
     * transfer the admin role to another member via
     * {@link #grantAdmin(UUID, UUID, User)}.
     *
     * @param spaceId the target space's UUID.
     * @param user    the authenticated user.
     * @throws ApiException 404 if the space or the user's membership does not
     *                      exist.
     * @throws ApiException 400 if the user is the sole remaining admin.
     */
    @Transactional
    public void leaveSpace(UUID spaceId, User user) {
        Space space = requireSpace(spaceId);

        SpaceMembership membership = membershipRepository.findBySpaceIdAndUserId(spaceId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "You are not a member of this space"));

        if (ROLE_ADMIN.equals(membership.getRole())) {
            long adminCount = membershipRepository.countBySpaceIdAndRole(spaceId, ROLE_ADMIN);
            if (adminCount <= 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "You are the only admin of this space. "
                                + "Please transfer the admin role to another member before leaving.");
            }
        }

        membershipRepository.delete(membership);

        space.setMemberCount(Math.max(0, space.getMemberCount() - 1));
        spaceRepository.save(space);
    }

    // ─── Edit ─────────────────────────────────────────────────────────────────

    /**
     * Partially updates the editable fields of a space.
     *
     * <p>
     * Only members with the {@code ADMIN} role may call this method.
     * A new slug is derived automatically if {@code name} is changed, and the
     * uniqueness of the new slug is guaranteed without conflicting with the
     * space's own current slug.
     *
     * @param spaceId   the UUID of the space to update.
     * @param request   the partial update payload (null fields are ignored).
     * @param requester the authenticated user performing the update.
     * @return the updated space as a response DTO.
     * @throws ApiException 404 if the space does not exist.
     * @throws ApiException 403 if the requester is not an admin of the space.
     */
    @Transactional
    public SpaceResponse updateSpace(UUID spaceId, UpdateSpaceRequest request, User requester) {
        Space space = requireSpace(spaceId);
        requireAdminRole(spaceId, requester);

        if (request.getName() != null) {
            space.setName(request.getName());
            String newSlug = generateUniqueSlug(request.getName(), spaceId);
            space.setSlug(newSlug);
        }

        Optional.ofNullable(request.getDescription()).ifPresent(space::setDescription);
        Optional.ofNullable(request.getCategory()).ifPresent(space::setCategory);
        Optional.ofNullable(request.getCourseCode()).ifPresent(space::setCourseCode);

        return toResponse(spaceRepository.save(space));
    }

    // ─── Grant Admin ──────────────────────────────────────────────────────────

    /**
     * Elevates an existing space member to the {@code ADMIN} role.
     *
     * <p>
     * The requester's own admin role is unaffected — this is a grant, not a
     * transfer. If the caller subsequently wants to step down they must leave
     * the space (which is only permitted once there is at least one other admin).
     *
     * @param spaceId   the UUID of the space.
     * @param memberId  the UUID of the user whose role should be elevated.
     * @param requester the authenticated user performing the action.
     * @return the updated membership record.
     * @throws ApiException 404 if the space does not exist, or the target user is
     *                      not a member.
     * @throws ApiException 403 if the requester is not an admin.
     */
    @Transactional
    public MembershipResponse grantAdmin(UUID spaceId, UUID memberId, User requester) {
        requireSpace(spaceId);
        requireAdminRole(spaceId, requester);

        SpaceMembership targetMembership = membershipRepository.findBySpaceIdAndUserId(spaceId, memberId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "User is not a member of this space"));

        targetMembership.setRole(ROLE_ADMIN);
        return toMembershipResponse(membershipRepository.save(targetMembership));
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    /**
     * Searches all active spaces with optional text filter, category filter,
     * and configurable sort over {@code memberCount} or {@code createdAt}.
     *
     * @param query    substring matched against name and description; {@code null}
     *                 or
     *                 blank means no text filter.
     * @param category optional category filter; {@code null} means all categories.
     * @param sortBy   field to sort by — {@code "memberCount"} or
     *                 {@code "createdAt"}
     *                 (default: {@code "createdAt"}).
     * @param sortDir  {@code "asc"} or {@code "desc"} (default: {@code "desc"}).
     * @param page     zero-based page index.
     * @param size     page size.
     * @return matching spaces as a list of {@link SpaceResponse}s.
     */
    @Transactional(readOnly = true)
    public List<SpaceResponse> searchSpaces(
            String query,
            SpaceCategory category,
            String sortBy,
            String sortDir,
            int page,
            int size) {

        Sort sort = buildSort(sortBy, sortDir, SPACE_SORT_FIELDS);
        PageRequest pageable = PageRequest.of(page, size, sort);

        // Normalise blank query to null so the JPQL IS NULL check skips the filter
        String normalizedQuery = (query == null || query.isBlank()) ? null : query.trim();

        return spaceRepository
                .searchSpaces(normalizedQuery, category, pageable)
                .getContent()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Loads a space by ID or throws 404.
     */
    private Space requireSpace(UUID spaceId) {
        return spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Space not found with id: " + spaceId));
    }

    /**
     * Verifies that {@code requester} holds the {@code ADMIN} role in the given
     * space.
     *
     * @throws ApiException 403 if the requester is not a member or is not an admin.
     */
    private void requireAdminRole(UUID spaceId, User requester) {
        SpaceMembership membership = membershipRepository
                .findBySpaceIdAndUserId(spaceId, requester.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "You are not a member of this space"));

        if (!ROLE_ADMIN.equals(membership.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Only admins can perform this action");
        }
    }

    /**
     * Builds a {@link Sort} from the supplied field name and direction, falling
     * back to {@code createdAt DESC} for unknown values.
     *
     * @param sortBy      requested sort field.
     * @param sortDir     {@code "asc"} or {@code "desc"}.
     * @param validFields set of accepted field names for this domain.
     */
    private Sort buildSort(String sortBy, String sortDir, Set<String> validFields) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        String field = (sortBy != null && validFields.contains(sortBy)) ? sortBy : "createdAt";
        return Sort.by(direction, field);
    }

    /**
     * Generates a unique slug for the given name, excluding {@code excludeSpaceId}
     * from the uniqueness check (used during updates so the space's own slug is
     * not treated as a conflict).
     *
     * @param name           the display name to slugify.
     * @param excludeSpaceId UUID of the space currently being updated, or
     *                       {@code null} for new spaces.
     * @return a slug that does not already exist in the database.
     */
    private String generateUniqueSlug(String name, UUID excludeSpaceId) {
        String base = SlugUtil.toSlug(name);
        String candidate = base;
        int suffix = 2;

        while (isSlugTaken(candidate, excludeSpaceId)) {
            candidate = SlugUtil.withSuffix(base, suffix++);
        }

        return candidate;
    }

    public boolean isMemberInSpace(UUID spaceId, UUID userId) {
        return membershipRepository.existsBySpaceIdAndUserId(spaceId, userId);
    }

    @Transactional
    public SpaceResponse getSpaceById(UUID spaceId, UUID userId) {
        if (isMemberInSpace(spaceId, userId)) {
            Space space = requireSpace(spaceId);
            return toResponse(space);
        }
        return null;
    }

    @Transactional
    public List<SpaceResponse> getSpacesByUserId(UUID userId) {
        List<SpaceMembership> memberships = membershipRepository.findByUserId(userId);
        return memberships.stream()
                .map(m -> toResponse(m.getSpace()))
                .toList();
    }

    @Transactional
    public List<SpaceResponse> getAllSpaces() {
        List<Space> spaces = spaceRepository.findAllActiveSpaces();
        return spaces.stream()
                .map(this::toResponse)
                .toList();
    }

    private boolean isSlugTaken(String slug, UUID excludeSpaceId) {
        if (excludeSpaceId == null) {
            return spaceRepository.existsBySlug(slug);
        }
        return spaceRepository.existsBySlugAndIdNot(slug, excludeSpaceId);
    }

    /**
     * Concatenates name and description for similarity computation.
     * Null description is treated as an empty string.
     */
    private String buildTextForSimilarity(String name, String description) {
        return (name == null ? "" : name)
                + " "
                + (description == null ? "" : description);
    }

    /**
     * Computes the Jaccard similarity coefficient between two texts.
     *
     * <p>
     * Both texts are tokenised into lowercase word-sets (tokens shorter than
     * 3 characters are discarded as noise). The coefficient is
     * {@code |intersection| / |union|}, ranging from 0.0 (no overlap) to
     * 1.0 (identical word sets).
     *
     * @param text1 first text (name + description of candidate space).
     * @param text2 second text (name + description of an existing space).
     * @return Jaccard similarity in [0.0, 1.0].
     */
    private double computeJaccardSimilarity(String text1, String text2) {
        Set<String> words1 = tokenize(text1);
        Set<String> words2 = tokenize(text2);

        if (words1.isEmpty() && words2.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * Splits text into a set of lowercase word tokens, discarding tokens shorter
     * than 3 characters to reduce noise from articles and prepositions.
     */
    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(token -> token.length() >= 3)
                .collect(Collectors.toSet());
    }

    /**
     * Maps a {@link Space} entity to its response DTO without a similarity score.
     */
    private SpaceResponse toResponse(Space space) {
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

    /**
     * Maps a {@link SpaceMembership} entity to its response DTO.
     *
     * <p>
     * Accesses {@code membership.getSpace()} and {@code membership.getUser()} —
     * both associations are {@code LAZY}, so this method must only be called
     * within an active transaction.
     */
    private MembershipResponse toMembershipResponse(SpaceMembership m) {
        return MembershipResponse.builder()
                .id(m.getId())
                .spaceId(m.getSpace().getId())
                .spaceName(m.getSpace().getName())
                .userId(m.getUser().getId())
                .userName(m.getUser().getFullName())
                .role(m.getRole())
                .joinedAt(m.getJoinedAt())
                .build();
    }
}