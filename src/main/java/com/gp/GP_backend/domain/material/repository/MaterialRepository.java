package com.gp.GP_backend.domain.material.repository;

import com.gp.GP_backend.domain.material.entity.Material;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for {@link Material} entities.
 */
public interface MaterialRepository extends JpaRepository<Material, UUID> {

    /** All materials in a space, newest first, paginated. */
    Page<Material> findBySpaceIdOrderByCreatedAtDesc(UUID spaceId, Pageable pageable);
}
