package com.gp.GP_backend.domain.space.repository;

import com.gp.GP_backend.domain.space.entity.Space;
import com.gp.GP_backend.domain.space.entity.SpaceCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;

public interface SpaceRepository extends JpaRepository<Space, UUID> {

        // =============================================================================
        // ADD THE FOLLOWING FOUR METHODS TO SpaceRepository.java
        // (inside the existing `public interface SpaceRepository extends
        // JpaRepository<Space, UUID>`)
        // =============================================================================

        // ─── Recommendation — Layer 1 (Course Match) ──────────────────────────────

        /**
         * Finds all active {@code COLLEGE_COURSE} spaces whose {@code courseCode} is in
         * {@code courseCodes} and that the given user has not yet joined.
         *
         * <p>
         * Ordered by {@code memberCount} descending so the most popular matching course
         * spaces surface first when all three have a score of 1.0.
         *
         * <p>
         * Uses {@code NOT EXISTS} instead of {@code NOT IN} for better SQL Server
         * performance on large membership tables.
         */
        @EntityGraph(attributePaths = { "createdBy" })
        @Query("""
                        SELECT s FROM Space s
                        WHERE  s.isActive    = true
                          AND  s.category    = :category
                          AND  s.courseCode  IN :courseCodes
                          AND  NOT EXISTS (
                                   SELECT m FROM SpaceMembership m
                                   WHERE  m.space.id = s.id
                                     AND  m.user.id  = :userId
                               )
                        ORDER BY s.memberCount DESC
                        """)
        List<Space> findCollegeCourseSpacesNotJoined(
                        @Param("courseCodes") List<String> courseCodes,
                        @Param("userId") UUID userId,
                        @Param("category") SpaceCategory category);

        // ─── Recommendation — Layer 3 (Text Similarity, user side) ───────────────

        /**
         * Returns all active spaces in any category <em>except</em>
         * {@code excludeCategory}
         * that the given user is currently a member of.
         *
         * <p>
         * Used to build the reference token sets for Jaccard similarity scoring.
         * {@code (s.category IS NULL OR s.category <> :excludeCategory)} correctly
         * handles
         * spaces that were created without a category.
         */
        @EntityGraph(attributePaths = { "createdBy" })
        @Query("""
                        SELECT DISTINCT m.space FROM SpaceMembership m
                        WHERE  m.user.id          = :userId
                          AND  m.space.isActive   = true
                          AND  (m.space.category IS NULL OR m.space.category <> :excludeCategory)
                        """)
        List<Space> findActiveNonCategorySpacesByUser(
                        @Param("userId") UUID userId,
                        @Param("excludeCategory") SpaceCategory excludeCategory);

        // ─── Recommendation — Layer 3 (Text Similarity, candidate side) ──────────

        /**
         * Returns all active spaces in any category <em>except</em>
         * {@code excludeCategory}
         * that the given user has <em>not</em> yet joined.
         *
         * <p>
         * These are the candidates compared against the user's token sets in Layer 3.
         */
        @EntityGraph(attributePaths = { "createdBy" })
        @Query("""
                        SELECT s FROM Space s
                        WHERE  s.isActive = true
                          AND  (s.category IS NULL OR s.category <> :excludeCategory)
                          AND  NOT EXISTS (
                                   SELECT m FROM SpaceMembership m
                                   WHERE  m.space.id = s.id
                                     AND  m.user.id  = :userId
                               )
                        """)
        List<Space> findActiveNonCategorySpacesNotJoined(
                        @Param("userId") UUID userId,
                        @Param("excludeCategory") SpaceCategory excludeCategory);

        // ─── Recommendation — Layer 2 (FoF batch load) ───────────────────────────

        /**
         * Batch-loads a list of spaces by their IDs, eagerly fetching their creators.
         *
         * <p>
         * Called after the FoF native query returns (spaceId, score) pairs: rather than
         * issuing one {@code SELECT} per space we consolidate into a single {@code IN}
         * query.
         *
         * <p>
         * <b>Caller must guard against an empty {@code ids} collection</b> — an empty
         * {@code IN} clause is a SQL syntax error. The recommendation service checks
         * this
         * before calling.
         */
        @EntityGraph(attributePaths = { "createdBy" })
        @Query("SELECT s FROM Space s WHERE s.id IN :ids")
        List<Space> findAllByIdWithCreator(@Param("ids") Collection<UUID> ids);

        // =============================================================================
        // REQUIRED IMPORTS to add at the top of SpaceRepository.java
        // =============================================================================

        /** Used to check slug uniqueness before creating a new space. */
        boolean existsBySlug(String slug);

        /**
         * Slug uniqueness check that excludes the space being updated.
         * Used during PATCH to avoid false conflicts with the space's own current slug.
         */
        boolean existsBySlugAndIdNot(String slug, UUID id);

        /** Used to check name uniqueness before creating a new space. */
        boolean existsByName(String name);

        /** Loads a space by its URL-safe slug (for space detail pages). */
        Optional<Space> findBySlug(String slug);

        /** Returns all active spaces, newest first, paginated. */
        @EntityGraph(attributePaths = { "createdBy" })
        Page<Space> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

        /**
         * Returns popular active spaces the user has not already joined.
         * Ordered by member count and recency so the Java recommendation layer can
         * seed exploration candidates without scanning the full table.
         */
        @Query("""
                        SELECT s FROM Space s
                        WHERE s.isActive = true
                                AND NOT EXISTS (
                                SELECT m FROM SpaceMembership m
                                WHERE m.space.id = s.id
                                AND m.user.id = :userId
                                )
                        """)
        Page<Space> findPopularSpacesNotJoined(@Param("userId") UUID userId, Pageable pageable);

        /** returns all spaces that share the same course code, newest first. */
        @EntityGraph(attributePaths = { "createdBy" })
        List<Space> findByCourseCodeOrderByCreatedAtDesc(String courseCode);

        /**
         * Used by the text-similarity duplicate check:
         * returns all active spaces in the given category for pairwise comparison.
         */
        @EntityGraph(attributePaths = { "createdBy" })
        List<Space> findByCategoryAndIsActiveTrue(SpaceCategory category);

        List<Space> findAll();

        @Query("SELECT s FROM Space s WHERE s.isActive = true")
        List<Space> findAllActiveSpaces();

        Optional<Space> findById(UUID id);

        /**
         * Loads a space while taking a database row lock.
         * Used to prevent race conditions (e.g., two admins leaving concurrently).
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT s FROM Space s WHERE s.id = :spaceId")
        Optional<Space> findByIdForUpdate(@Param("spaceId") UUID spaceId);

        /**
         * Atomically increments memberCount to avoid lost updates under concurrency.
         */
        @Modifying
        @Query("UPDATE Space s SET s.memberCount = s.memberCount + 1 WHERE s.id = :spaceId")
        int incrementMemberCount(@Param("spaceId") UUID spaceId);

        /** Atomically decrements memberCount, guarding against negative values. */
        @Modifying
        @Query("UPDATE Space s SET s.memberCount = CASE WHEN s.memberCount > 0 THEN s.memberCount - 1 ELSE 0 END WHERE s.id = :spaceId")
        int decrementMemberCount(@Param("spaceId") UUID spaceId);

        @Query("SELECT s.name FROM Space s WHERE s.id = :spaceId")
        String findNameById(@Param("spaceId") UUID spaceId);

        /**
         * Full-text search across name and description with optional category filter.
         * All parameters are optional — passing {@code null} skips that filter.
         * Sorting and pagination are driven by the supplied {@link Pageable}.
         */
        @Query("""
                        SELECT s FROM Space s
                        WHERE s.isActive = true
                        AND (:query IS NULL OR s.name LIKE CONCAT('%', :query, '%')
                            OR s.description LIKE CONCAT('%', :query, '%'))
                        AND (:category IS NULL OR s.category = :category)
                        """)
        @EntityGraph(attributePaths = { "createdBy" })
        Page<Space> searchSpaces(
                        @Param("query") String query,
                        @Param("category") SpaceCategory category,
                        Pageable pageable);
}