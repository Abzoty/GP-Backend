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
    @Query("""
    SELECT m.uploadedBy.id, COUNT(m)
    FROM Material m
    WHERE m.space.id = :spaceId
    GROUP BY m.uploadedBy.id
    """)
List<Object[]> countBySpaceIdGroupByUploader(@Param("spaceId") UUID spaceId);
}