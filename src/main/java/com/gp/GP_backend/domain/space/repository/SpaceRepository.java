package com.gp.GP_backend.domain.space.repository;

import com.gp.GP_backend.domain.space.entity.Space;
import com.gp.GP_backend.domain.space.entity.SpaceCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
    Page<Space> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

     /** returns all spaces that share the same course code, newest first. */
    List<Space> findByCourseCodeOrderByCreatedAtDesc(String courseCode);

    /**
     * Used by the text-similarity duplicate check:
     * returns all active spaces in the given category for pairwise comparison.
     */
    List<Space> findByCategoryAndIsActiveTrue(SpaceCategory category);
}