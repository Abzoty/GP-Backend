package com.gp.GP_backend.domain.space.repository;

import com.gp.GP_backend.domain.space.entity.SpaceMembership;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpaceMembershipRepository extends JpaRepository<SpaceMembership, UUID> {

    /**
     * Looks up a single membership by space and user — used for role checks and
     * leave/join guards.
     */
    Optional<SpaceMembership> findBySpaceIdAndUserId(UUID spaceId, UUID userId);

    /** All memberships for a space — used when listing members. */
    List<SpaceMembership> findBySpaceId(UUID spaceId);

    /**
     * All memberships with a specific role in a space — used for admin-count guard.
     */
    List<SpaceMembership> findBySpaceIdAndRole(UUID spaceId, String role);

    /**
     * How many members hold the given role in a space — used to guard the
     * last-admin case.
     */
    long countBySpaceIdAndRole(UUID spaceId, String role);

    /** Whether a user already belongs to a space — duplicate-join guard. */
    boolean existsBySpaceIdAndUserId(UUID spaceId, UUID userId);

    List<SpaceMembership> findByUserId(UUID userId);

    @EntityGraph(attributePaths = { "user" })
    List<SpaceMembership> findBySpace_Id(UUID spaceId);
}