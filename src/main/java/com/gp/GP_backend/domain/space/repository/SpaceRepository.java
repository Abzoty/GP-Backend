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
        @EntityGraph(attributePaths = { "createdBy" })
        @Query("SELECT s FROM Space s WHERE s.id IN :ids")
        List<Space> findAllByIdWithCreator(@Param("ids") Collection<UUID> ids);

        boolean existsBySlug(String slug);

        boolean existsBySlugAndIdNot(String slug, UUID id);

        boolean existsByName(String name);

        Optional<Space> findBySlug(String slug);

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
         * returns all active spaces in the given category.
         */
        @EntityGraph(attributePaths = { "createdBy" })
        List<Space> findByCategoryAndIsActiveTrue(SpaceCategory category);

        List<Space> findAll();

        @Query("SELECT s FROM Space s WHERE s.isActive = true")
        List<Space> findAllActiveSpaces();

        Optional<Space> findById(UUID id);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT s FROM Space s WHERE s.id = :spaceId")
        Optional<Space> findByIdForUpdate(@Param("spaceId") UUID spaceId);

        @Modifying
        @Query("UPDATE Space s SET s.memberCount = s.memberCount + 1 WHERE s.id = :spaceId")
        int incrementMemberCount(@Param("spaceId") UUID spaceId);

        @Modifying
        @Query("UPDATE Space s SET s.memberCount = CASE WHEN s.memberCount > 0 THEN s.memberCount - 1 ELSE 0 END WHERE s.id = :spaceId")
        int decrementMemberCount(@Param("spaceId") UUID spaceId);

        @Query("SELECT s.name FROM Space s WHERE s.id = :spaceId")
        String findNameById(@Param("spaceId") UUID spaceId);

        /**
         * Full-text search across name and description with optional category filter.
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