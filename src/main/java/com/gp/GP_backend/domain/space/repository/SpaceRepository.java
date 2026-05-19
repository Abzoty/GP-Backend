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

public interface SpaceRepository extends JpaRepository<Space, UUID> {

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

    /** Atomically increments memberCount to avoid lost updates under concurrency. */
    @Modifying
    @Query("UPDATE Space s SET s.memberCount = s.memberCount + 1 WHERE s.id = :spaceId")
    int incrementMemberCount(@Param("spaceId") UUID spaceId);

    /** Atomically decrements memberCount, guarding against negative values. */
    @Modifying
    @Query("UPDATE Space s SET s.memberCount = CASE WHEN s.memberCount > 0 THEN s.memberCount - 1 ELSE 0 END WHERE s.id = :spaceId")
    int decrementMemberCount(@Param("spaceId") UUID spaceId);
    @Query("SELECT s.name FROM Space s WHERE s.id = :spaceId")
    String findNameById(@Param("spaceId")UUID spaceId);

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