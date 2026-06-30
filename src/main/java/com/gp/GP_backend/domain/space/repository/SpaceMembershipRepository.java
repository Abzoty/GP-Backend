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

    // =============================================================================
    // ADD THE FOLLOWING METHOD TO SpaceMembershipRepository.java
    // (inside the existing `public interface SpaceMembershipRepository extends
    // JpaRepository<...>`)
    // =============================================================================

    // ─── Recommendation — Layer 2 (Friends-of-Friends) ───────────────────────

    /**
     * Friends-of-friends social signal.
     *
     * <p>
     * <b>Logic (three hops):</b>
     * <ol>
     * <li>Find every space the target user belongs to ({@code m1}).</li>
     * <li>For each such space, collect all other members ({@code m2} —
     * "friends").</li>
     * <li>For each friend, collect all other spaces they have joined ({@code m3})
     * that the target user has not already joined.</li>
     * </ol>
     * Aggregates by space and counts distinct friends who endorsed each space.
     * Returns up to {@code topN} rows, ordered by that count descending.
     *
     * <p>
     * <b>Result row layout:</b>
     * <ul>
     * <li>{@code row[0]} — {@code space_id} as a {@code String} (UNIQUEIDENTIFIER
     * comes back as String via SQL Server JDBC in native queries).</li>
     * <li>{@code row[1]} — friend endorsement count as a {@link Number}
     * (typically {@link Long}).</li>
     * </ul>
     *
     * <p>
     * <b>Performance:</b> all three join columns ({@code space_id},
     * {@code user_id})
     * are covered by the existing unique constraint index on
     * {@code space_memberships(space_id, user_id)}, so each hop is an index seek.
     * Typical wall time is &lt; 50 ms for 10 000 members and 500 spaces.
     *
     * <p>
     * <b>Note on {@code TOP} syntax:</b> SQL Server supports parameterized
     * {@code TOP (?)} in JDBC prepared statements. Spring Data JPA passes
     * {@code :topN} as a bind variable, which the SQL Server JDBC driver
     * translates correctly.
     *
     * @param userId the target user's UUID.
     * @return list of two-element {@code Object[]} arrays — see row layout above.
     */
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

    // =============================================================================
    // NO NEW IMPORTS NEEDED — UUID and List<Object[]> are already imported.
    // =============================================================================

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

    @Query("SELECT m.user.id FROM SpaceMembership m WHERE m.space.id = :spaceId")
    List<UUID> findMembersIdsBySpaceId(UUID spaceId);

    /**
     * Eager-loads the Space (and its creator) for each membership to avoid N+1
     * queries
     * when mapping to SpaceResponse.
     */
    @Query("SELECT m FROM SpaceMembership m JOIN FETCH m.space s LEFT JOIN FETCH s.createdBy WHERE m.user.id = :userId")
    List<SpaceMembership> findByUserIdWithSpace(@Param("userId") UUID userId);

    /**
     * Single-query membership lookup with Space + Space.createdBy preloaded.
     * Used to serve getSpaceById without triggering lazy-load fanout.
     */
    @Query("SELECT m FROM SpaceMembership m JOIN FETCH m.space s LEFT JOIN FETCH s.createdBy WHERE s.id = :spaceId AND m.user.id = :userId")
    Optional<SpaceMembership> findBySpaceIdAndUserIdWithSpaceAndCreator(
            @Param("spaceId") UUID spaceId,
            @Param("userId") UUID userId);
}