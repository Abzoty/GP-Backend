package com.gp.GP_backend.domain.material.repository;

import com.gp.GP_backend.domain.material.entity.MaterialLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface MaterialLinkRepository extends JpaRepository<MaterialLink, UUID> {

        // All materials bookmarked by a user within a specific space.
        @Query("""
                        SELECT ml FROM MaterialLink ml
                        WHERE ml.user.id = :userId
                        AND ml.material.space.id = :spaceId
                        ORDER BY ml.linkedAt DESC
                        """)
        List<MaterialLink> findByUserIdAndSpaceId(
                        @Param("userId") UUID userId,
                        @Param("spaceId") UUID spaceId);

        // Duplicate-bookmark guard: checks whether a user has already bookmarked a material.
        boolean existsByMaterialIdAndUserId(UUID materialId, UUID userId);

        // Finds a specific bookmark record for removal (un-bookmark).
        Optional<MaterialLink> findByMaterialIdAndUserId(UUID materialId, UUID userId);

        // Bulk-deletes all bookmarks for a given material.
        @Modifying
        @Transactional
        @Query("DELETE FROM MaterialLink ml WHERE ml.material.id = :materialId")
        void deleteByMaterialId(@Param("materialId") UUID materialId);

        // Fetches all bookmarks for a specific user out of a given list of materials in
        // ONE query
        @Query("SELECT ml.material.id FROM MaterialLink ml WHERE ml.user.id = :userId AND ml.material.id IN :materialIds")
        java.util.Set<UUID> findBookmarkedMaterialIds(@Param("userId") UUID userId,
                        @Param("materialIds") java.util.List<UUID> materialIds);
}