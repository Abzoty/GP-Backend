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
import org.springframework.dao.DataIntegrityViolationException;
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

    private static final String ROLE_ADMIN = "ADMIN";

    private static final String ROLE_MEMBER = "MEMBER";

    private static final Set<String> SPACE_SORT_FIELDS = Set.of("memberCount", "createdAt");

    private final SpaceRepository spaceRepository;
    private final SpaceMembershipRepository membershipRepository;

    // ─── Similarity check ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SpaceResponse> checkSimilarity(CreateSpaceRequest request) {

        if (request.getCategory() == SpaceCategory.COLLEGE_COURSE) {
            return checkCollegeCourseConflict(request);
        }

        return checkTextSimilarityConflict(request);
    }


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
                .filter(r -> r.getSimilarityScore() > 0.2) // threshold for "conflicting" similarity; tune as needed
                .sorted(Comparator.comparingDouble(SpaceResponse::getSimilarityScore).reversed())
                .toList();
    }

    // ─── Create ───────────────────────────────────────────────────────────────

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

            try {
                space = spaceRepository.save(space);
            } catch (DataIntegrityViolationException ex) {
                throw new ApiException(HttpStatus.CONFLICT,
                    "A space named '" + request.getName() + "' already exists");
            }

        // Automatically enrol the creator as ADMIN
        SpaceMembership adminMembership = SpaceMembership.builder()
                .space(space)
                .user(creator)
                .role(ROLE_ADMIN)
                .build();
        try {
            membershipRepository.save(adminMembership);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Failed to create admin membership for the space creator");
        }

        return toResponse(space);
    }

    // ─── Join ─────────────────────────────────────────────────────────────────

    @Transactional
    public MembershipResponse joinSpace(UUID spaceId, User user) {
        Space space = requireSpace(spaceId);

        SpaceMembership membership = SpaceMembership.builder()
                .space(space)
                .user(user)
                .role(ROLE_MEMBER)
                .build();
        try {
            membership = membershipRepository.save(membership);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "You are already a member of this space");
        }

        spaceRepository.incrementMemberCount(spaceId);

        return toMembershipResponse(membership);
    }

    // ─── Leave ────────────────────────────────────────────────────────────────

    @Transactional
    public void leaveSpace(UUID spaceId, User user) {
        spaceRepository.findByIdForUpdate(spaceId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "Space not found with id: " + spaceId));

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

        spaceRepository.decrementMemberCount(spaceId);
    }

    // ─── Edit ─────────────────────────────────────────────────────────────────

    @Transactional
    public SpaceResponse updateSpace(UUID spaceId, UpdateSpaceRequest request, User requester) {

        Space space = requireSpace(spaceId);
        requireAdminRole(spaceId, requester);

        if (request.getName() != null && !request.getName().equals(space.getName())) {
            if (spaceRepository.existsByName(request.getName())) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "A space named '" + request.getName() + "' already exists");
            }
            space.setName(request.getName());
            String newSlug = generateUniqueSlug(request.getName(), spaceId);
            space.setSlug(newSlug);
        }

        Optional.ofNullable(request.getDescription()).ifPresent(space::setDescription);
        Optional.ofNullable(request.getCategory()).ifPresent(space::setCategory);
        Optional.ofNullable(request.getCourseCode()).ifPresent(space::setCourseCode);

        try {
            return toResponse(spaceRepository.save(space));
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "A space with the provided name or slug already exists");
        }
    }

    // ─── Grant Admin ──────────────────────────────────────────────────────────

    @Transactional
    public MembershipResponse grantAdmin(UUID spaceId, UUID memberId, User requester) {
        requireSpace(spaceId);
        requireAdminRole(spaceId, requester);

        SpaceMembership targetMembership = membershipRepository.findBySpaceIdAndUserId(spaceId, memberId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "User is not a member of this space"));
        if (ROLE_ADMIN.equals(targetMembership.getRole())) {
            return toMembershipResponse(targetMembership); // already an admin, no-op
        }
        targetMembership.setRole(ROLE_ADMIN);
        return toMembershipResponse(membershipRepository.save(targetMembership));
    }

    // ─── Search ───────────────────────────────────────────────────────────────

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

        String normalizedQuery = (query == null || query.isBlank()) ? null : query.trim();

        return spaceRepository
                .searchSpaces(normalizedQuery, category, pageable)
                .getContent()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private Space requireSpace(UUID spaceId) {
        return spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Space not found with id: " + spaceId));
    }

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

    private Sort buildSort(String sortBy, String sortDir, Set<String> validFields) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        String field = (sortBy != null && validFields.contains(sortBy)) ? sortBy : "createdAt";
        return Sort.by(direction, field);
    }

    private String generateUniqueSlug(String name, UUID excludeSpaceId) {
        String base = SlugUtil.toSlug(name);
        String candidate = base;
        int suffix = 2;

        while (isSlugTaken(candidate, excludeSpaceId)) {
            candidate = SlugUtil.withSuffix(base, suffix++);
        }

        return candidate;
    }

    @Transactional(readOnly = true)
    public boolean isMemberInSpace(UUID spaceId, UUID userId) {
        return membershipRepository.existsBySpaceIdAndUserId(spaceId, userId);
    }

    @Transactional(readOnly = true)
    public SpaceResponse getSpaceById(UUID spaceId, UUID userId) {
        SpaceMembership membership = membershipRepository.findBySpaceIdAndUserIdWithSpaceAndCreator(spaceId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this space"));
        SpaceResponse resp = toResponse(membership.getSpace());
        resp.setRole(membership.getRole());
        return resp;
    }

    @Transactional(readOnly = true)
    public List<SpaceResponse> getSpacesByUserId(UUID userId) {
        List<SpaceMembership> memberships = membershipRepository.findByUserIdWithSpace(userId);
        return memberships.stream()
                .map(m -> {
                    SpaceResponse resp = toResponse(m.getSpace());
                    resp.setRole(m.getRole());
                    return resp;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MembershipResponse> getSpaceMembers(UUID spaceId, UUID userId) {
        requireSpace(spaceId);
        if (!isMemberInSpace(spaceId, userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this space");
        }
        return membershipRepository.findBySpace_Id(spaceId)
                .stream()
                .map(this::toMembershipResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SpaceResponse> getActiveSpaces(int page, int size) {
        List<Space> spaces = spaceRepository.findByIsActiveTrueOrderByCreatedAtDesc(PageRequest.of(page, size))
                .getContent();
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

    private String buildTextForSimilarity(String name, String description) {
        return (name == null ? "" : name)
                + " "
                + (description == null ? "" : description);
    }

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

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(token -> token.length() >= 3)
                .collect(Collectors.toSet());
    }

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