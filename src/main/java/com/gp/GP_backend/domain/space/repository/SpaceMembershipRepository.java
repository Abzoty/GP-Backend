package com.gp.GP_backend.domain.space.repository;

import com.gp.GP_backend.domain.space.entity.Space;
import com.gp.GP_backend.domain.space.entity.SpaceMembership;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpaceMembershipRepository extends JpaRepository<SpaceMembership, UUID> {

    @Query(value = """
            SELECT TOP 200
                    m3.space_id,
                    COUNT(DISTINCT m2.user_id) AS score
            FROM   space_memberships m1
                    INNER JOIN space_memberships m2
                        ON  m2.space_id = m1.space_id
                        AND m2.user_id  <> :userId
                    INNER JOIN space_memberships m3
                        ON  m3.user_id  = m2.user_id
                    INNER JOIN spaces s
                        ON  s.id        = m3.space_id
                        AND s.is_active = 1
            WHERE  m1.user_id = :userId
                AND  NOT EXISTS (
                        SELECT 1
                        FROM   space_memberships x
                        WHERE  x.space_id = m3.space_id
                        AND  x.user_id  = :userId
                    )
            GROUP BY m3.space_id
            ORDER BY score DESC
            """, nativeQuery = true)
    List<Object[]> findFofSpaceScores(@Param("userId") UUID userId);

    Optional<SpaceMembership> findBySpaceIdAndUserId(UUID spaceId, UUID userId);

    List<SpaceMembership> findBySpaceId(UUID spaceId);

    List<SpaceMembership> findBySpaceIdAndRole(UUID spaceId, String role);

    /**
     * How many members hold the given role in a space — used to guard the
     * last-admin case.
     */
    long countBySpaceIdAndRole(UUID spaceId, String role);

    boolean existsBySpaceIdAndUserId(UUID spaceId, UUID userId);

    List<SpaceMembership> findByUserId(UUID userId);

    @EntityGraph(attributePaths = { "user" })
    List<SpaceMembership> findBySpace_Id(UUID spaceId);

    @Query("SELECT m.user.id FROM SpaceMembership m WHERE m.space.id = :spaceId")
    List<UUID> findMembersIdsBySpaceId(UUID spaceId);

    @Query("SELECT m FROM SpaceMembership m JOIN FETCH m.space s LEFT JOIN FETCH s.createdBy WHERE m.user.id = :userId")
    List<SpaceMembership> findByUserIdWithSpace(@Param("userId") UUID userId);

    @Query("SELECT m FROM SpaceMembership m JOIN FETCH m.space s LEFT JOIN FETCH s.createdBy WHERE s.id = :spaceId AND m.user.id = :userId")
    Optional<SpaceMembership> findBySpaceIdAndUserIdWithSpaceAndCreator(
            @Param("spaceId") UUID spaceId,
            @Param("userId") UUID userId);
}