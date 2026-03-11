package com.gp.GP_backend.domain.space.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Business logic for space creation, membership, and management.
 *
 * TODO: Implement:
 * - createSpace(CreateSpaceRequest, User creator) — generates slug, saves space
 * + OWNER membership
 * - joinSpace(UUID spaceId, User user) — creates MEMBER SpaceMembership,
 * increments member_count
 * - leaveSpace(UUID spaceId, User user) — removes membership, decrements
 * member_count
 * - getSpaces(Pageable) — returns paginated SpaceResponse list
 * - getSpaceBySlug(String slug) — returns SpaceResponse or 404
 */
@Service
@RequiredArgsConstructor
public class SpaceService {
    // TODO: inject SpaceRepository, SpaceMembershipRepository, UserService,
    // SlugUtil
}
