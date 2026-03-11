package com.gp.GP_backend.domain.space.repository;

import com.gp.GP_backend.domain.space.entity.Space;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Space} entities.
 */
public interface SpaceRepository extends JpaRepository<Space, UUID> {

    /** Used to check slug uniqueness before creating a new space. */
    boolean existsBySlug(String slug);

    /** Loads a space by its URL-safe slug (for space detail pages). */
    Optional<Space> findBySlug(String slug);

    /** Returns all active spaces, newest first, paginated. */
    Page<Space> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);
}
