package com.gp.GP_backend.domain.material.repository;

import com.gp.GP_backend.domain.material.entity.Material;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MaterialRepository extends JpaRepository<Material, UUID> {

    /**
     * All materials in a space ordered by creation date, newest first —
     * paginated variant for use when large spaces require pagination.
     */
    Page<Material> findBySpaceIdOrderByCreatedAtDesc(UUID spaceId, Pageable pageable);

    /**
     * All materials in a space ordered by creation date, newest first —
     * full list variant used by the "get all materials in a space" endpoint.
     */
    List<Material> findBySpaceIdOrderByCreatedAtDesc(UUID spaceId);

    Integer countBySpaceIdAndUploadedById(UUID spaceId, UUID uid);

    /**
     * Searches materials within a space by title or description with an optional
     * resource-type filter.
     * All parameters except {@code spaceId} are optional — passing {@code null}
     * skips that filter.
     * Sorting and pagination are driven by the supplied {@link Pageable}.
     */
    @Query("""
            SELECT m FROM Material m
            WHERE m.space.id = :spaceId
            AND (:query IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%')))
            AND (:resourceType IS NULL OR m.resourceType = :resourceType)
            """)
    Page<Material> searchMaterials(
            @Param("spaceId") UUID spaceId,
            @Param("query") String query,
            @Param("resourceType") String resourceType,
            Pageable pageable);
}