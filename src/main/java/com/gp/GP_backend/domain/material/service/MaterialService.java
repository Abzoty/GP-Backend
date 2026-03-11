package com.gp.GP_backend.domain.material.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Handles material upload metadata, linking, and retrieval.
 *
 * TODO: Implement:
 * - shareMaterial(CreateMaterialRequest, User uploader) — saves Material,
 * awards XP_MATERIAL_SHARED
 * - getMaterialsBySpace(UUID spaceId, Pageable) — returns paginated
 * MaterialResponse
 * - linkMaterial(UUID materialId, User user) — creates MaterialLink, increments
 * link_count
 * - unlinkMaterial(UUID materialId, User user) — removes MaterialLink,
 * decrements link_count
 *
 * Note: Actual file storage (S3 / Azure Blob) is out of scope here.
 * The {@code url} field holds the pre-signed or permanent URL from the storage
 * layer.
 */
@Service
@RequiredArgsConstructor
public class MaterialService {
    // TODO: inject MaterialRepository, MaterialLinkRepository, GamificationService
}
