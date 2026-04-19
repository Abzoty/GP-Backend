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

/**
 * Repository for {@link MaterialLink} entities (bookmarks).
 *
 * <p>
 * A {@code MaterialLink} records that a user has bookmarked a material.
 * Its existence also drives the {@code linkCount} denormalised counter on
 * {@link com.gp.GP_backend.domain.material.entity.Material}.
 */
public interface MaterialLinkRepository extends JpaRepository<MaterialLink, UUID> {

    /**
     * All materials bookmarked by a user within a specific space.
     * Used by the "bookmarked materials" endpoint.
     *
     * <p>
     * A JPQL query is used instead of a derived method name because the
     * traversal crosses two associations ({@code material → space}), which
     * Spring Data's method-name parser does not handle reliably at three levels.
     */
    @Query("""
            SELECT ml FROM MaterialLink ml
            WHERE ml.user.id = :userId
            AND ml.material.space.id = :spaceId
            ORDER BY ml.linkedAt DESC
            """)
    List<MaterialLink> findByUserIdAndSpaceId(
            @Param("userId") UUID userId,
            @Param("spaceId") UUID spaceId);

    /**
     * Duplicate-bookmark guard: checks whether a user has already bookmarked a
     * material.
     */
    boolean existsByMaterialIdAndUserId(UUID materialId, UUID userId);

    /**
     * Finds a specific bookmark record for removal (unbookmark).
     */
    Optional<MaterialLink> findByMaterialIdAndUserId(UUID materialId, UUID userId);

    /**
     * Bulk-deletes all bookmarks for a given material.
     * Called during material deletion to cascade-clean the bookmark table.
     *
     * <p>
     * A {@code @Modifying @Query} is used for a single-statement DELETE
     * instead of Spring Data's derived delete (which issues N DELETEs).
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM MaterialLink ml WHERE ml.material.id = :materialId")
    void deleteByMaterialId(@Param("materialId") UUID materialId);
}