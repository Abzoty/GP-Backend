package com.gp.GP_backend.domain.material.repository;

import com.gp.GP_backend.domain.material.entity.MaterialLink;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
                //  ADD — paginated version of the same query
                @Query("""
                                SELECT ml FROM MaterialLink ml
                                WHERE ml.user.id = :userId
                                AND ml.material.space.id = :spaceId
                                ORDER BY ml.linkedAt DESC
                                """)
                Page<MaterialLink> findByUserIdAndSpaceId(
                                @Param("userId") UUID userId,
                                @Param("spaceId") UUID spaceId,
                                Pageable pageable);

        // Duplicate-bookmark guard: checks whether a user has already bookmarked a material.
        boolean existsByMaterialIdAndUserId(UUID materialId, UUID userId);

        // Finds a specific bookmark record for removal (un-bookmark).
        Optional<MaterialLink> findByMaterialIdAndUserId(UUID materialId, UUID userId);

        // Bulk-deletes all bookmarks for a given material.
        @Modifying
        @Transactional
        @Query("DELETE FROM MaterialLink ml WHERE ml.material.id = :materialId")
        void deleteByMaterialId(@Param("materialId") UUID materialId);
        // ✅ Fix — push the increment into the database
@Modifying
@Query("UPDATE Material m SET m.linkCount = m.linkCount + 1 WHERE m.id = :id")
void incrementLinkCount(@Param("id") UUID id);

@Modifying
@Query("UPDATE Material m SET m.linkCount = GREATEST(m.linkCount - 1, 0) WHERE m.id = :id")
void decrementLinkCount(@Param("id") UUID id);
}