package com.gp.GP_backend.domain.material.service;

import com.gp.GP_backend.domain.material.dto.EditMaterialRequest;
import com.gp.GP_backend.domain.material.dto.MaterialResponse;
import com.gp.GP_backend.domain.material.dto.ShareLinkRequest;
import com.gp.GP_backend.domain.material.entity.AcceptedFileType;
import com.gp.GP_backend.domain.material.entity.Material;
import com.gp.GP_backend.domain.material.entity.MaterialLink;
import com.gp.GP_backend.domain.material.repository.MaterialLinkRepository;
import com.gp.GP_backend.domain.material.repository.MaterialRepository;
import com.gp.GP_backend.domain.space.entity.Space;
import com.gp.GP_backend.domain.space.repository.SpaceMembershipRepository;
import com.gp.GP_backend.domain.space.repository.SpaceRepository;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.exception.ApiException;
import com.gp.GP_backend.shared.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Business logic for material uploads, link sharing, bookmarking, editing,
 * deletion, and retrieval.
 *
 * <p>
 * <b>Resource type conventions:</b>
 * <ul>
 * <li>File materials: {@code resourceType} = the {@link AcceptedFileType} enum
 * name (e.g. {@code "PDF"}, {@code "DOCX"}); {@code url} = stored
 * filename.</li>
 * <li>Link materials: {@code resourceType} = {@code "LINK"};
 * {@code url} = the full external URL.</li>
 * </ul>
 *
 * <p>
 * <b>Membership rule:</b> every mutating operation and every read requires
 * the acting user to be a member of the target space.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialService {

    /**
     * Literal stored in {@code Material.resourceType} for external-link materials.
     */
    private static final String RESOURCE_TYPE_LINK = "LINK";

    private final MaterialRepository materialRepository;
    private final MaterialLinkRepository materialLinkRepository;
    private final SpaceMembershipRepository spaceMembershipRepository;
    private final SpaceRepository spaceRepository;
    private final FileStorageService fileStorageService;

    // ─── Upload file ──────────────────────────────────────────────────────────

    /**
     * Validates, stores, and registers an uploaded file as a material in a space.
     *
     * <p>
     * File type and size validation are delegated to
     * {@link FileStorageService#store}.
     * Membership in the target space is verified before any work is done.
     *
     * @param spaceId     the space to post the material in.
     * @param title       display title for the material (trimmed).
     * @param description optional description.
     * @param file        the multipart file from the HTTP request.
     * @param uploader    the authenticated user performing the upload.
     * @return the persisted material as a response DTO.
     * @throws ApiException 403 if the uploader is not a space member.
     * @throws ApiException 400/415 if the file fails validation (size / type).
     */
    @Transactional
    public MaterialResponse uploadFile(UUID spaceId,
            String title,
            String description,
            MultipartFile file,
            User uploader) {
        Space space = requireMemberSpace(spaceId, uploader.getId());

        // store() validates type and size, then writes to disk and returns the filename
        String filename = fileStorageService.store(file);

        // Safe: store() already validated the type, so get() will not fail
        AcceptedFileType fileType = AcceptedFileType.fromMimeType(file.getContentType()).get();
        int fileSizeKb = (int) Math.ceil(file.getSize() / 1024.0);

        Material material = Material.builder()
                .space(space)
                .uploadedBy(uploader)
                .title(title.trim())
                .description(description)
                .resourceType(fileType.name()) // e.g. "PDF", "DOCX"
                .url(filename) // stored filename on disk
                .fileSizeKb(fileSizeKb)
                .build();

        Material saved = materialRepository.save(material);
        log.debug("File material created: id={}, type={}, file={}", saved.getId(), fileType.name(), filename);

        return toResponse(saved, false);
    }

    // ─── Share link ────────────────────────────────────────────────────────────

    /**
     * Registers an external URL as a material in a space.
     *
     * <p>
     * No file is stored — only the URL and metadata are persisted.
     *
     * @param request  the link-sharing payload.
     * @param uploader the authenticated user sharing the link.
     * @return the persisted material as a response DTO.
     * @throws ApiException 403 if the uploader is not a space member.
     */
    @Transactional
    public MaterialResponse shareLink(ShareLinkRequest request, User uploader) {
        Space space = requireMemberSpace(request.getSpaceId(), uploader.getId());

        Material material = Material.builder()
                .space(space)
                .uploadedBy(uploader)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .resourceType(RESOURCE_TYPE_LINK)
                .url(request.getUrl())
                .fileSizeKb(null) // links have no file size
                .build();

        Material saved = materialRepository.save(material);
        log.debug("Link material created: id={}, url={}", saved.getId(), request.getUrl());

        return toResponse(saved, false);
    }

    // ─── Bookmark (add) ────────────────────────────────────────────────────────

    /**
     * Bookmarks a material for the authenticated user and increments its
     * {@code linkCount}.
     *
     * @param materialId the material to bookmark.
     * @param user       the authenticated user.
     * @throws ApiException 403 if the user is not a member of the material's space.
     * @throws ApiException 409 if the user has already bookmarked this material.
     */
    @Transactional
    public void bookmark(UUID materialId, User user) {
        Material material = requireMaterial(materialId);
        requireMember(material.getSpace().getId(), user.getId());

        if (materialLinkRepository.existsByMaterialIdAndUserId(materialId, user.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "You have already bookmarked this material");
        }

        MaterialLink link = MaterialLink.builder()
                .material(material)
                .user(user)
                .build();
        materialLinkRepository.save(link);

        // Increment the denormalised bookmark counter
        material.setLinkCount(material.getLinkCount() + 1);
        materialRepository.save(material);

        log.debug("Material {} bookmarked by user {}", materialId, user.getId());
    }

    // ─── Bookmark (remove) ────────────────────────────────────────────────────

    /**
     * Removes a bookmark and decrements the material's {@code linkCount}.
     *
     * @param materialId the material to un-bookmark.
     * @param user       the authenticated user.
     * @throws ApiException 403 if the user is not a member of the material's space.
     * @throws ApiException 404 if the bookmark does not exist.
     */
    @Transactional
    public void unbookmark(UUID materialId, User user) {
        Material material = requireMaterial(materialId);
        requireMember(material.getSpace().getId(), user.getId());

        MaterialLink link = materialLinkRepository.findByMaterialIdAndUserId(materialId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "You have not bookmarked this material"));

        materialLinkRepository.delete(link);

        // Decrement the counter, guarding against going below zero
        material.setLinkCount(Math.max(0, material.getLinkCount() - 1));
        materialRepository.save(material);

        log.debug("Bookmark removed: material={}, user={}", materialId, user.getId());
    }

    // ─── Edit ─────────────────────────────────────────────────────────────────

    /**
     * Applies a partial update to a material's {@code title} and/or
     * {@code description}. Only the uploader may edit.
     *
     * <p>
     * Null fields in {@code request} are ignored (PATCH semantics).
     *
     * @param materialId the material to update.
     * @param request    the partial update payload.
     * @param user       the authenticated user.
     * @return the updated material as a response DTO.
     * @throws ApiException 404 if the material does not exist.
     * @throws ApiException 403 if the requester is not the uploader.
     */
    @Transactional
    public MaterialResponse editMaterial(UUID materialId, EditMaterialRequest request, User user) {
        Material material = requireMaterial(materialId);

        if (!material.getUploadedBy().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Only the uploader can edit this material");
        }

        Optional.ofNullable(request.getTitle())
                .filter(t -> !t.isBlank())
                .map(String::trim)
                .ifPresent(material::setTitle);

        Optional.ofNullable(request.getDescription())
                .ifPresent(material::setDescription);

        Material saved = materialRepository.save(material);
        boolean isBookmarked = materialLinkRepository.existsByMaterialIdAndUserId(materialId, user.getId());

        log.debug("Material {} edited by user {}", materialId, user.getId());
        return toResponse(saved, isBookmarked);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    /**
     * Deletes a material and all associated data.
     *
     * <p>
     * Deletion order:
     * <ol>
     * <li>All {@link MaterialLink} bookmarks are removed from the DB.</li>
     * <li>If the material is a file (not a link), the file is deleted from
     * disk.</li>
     * <li>The {@link Material} record itself is removed from the DB.</li>
     * </ol>
     *
     * <p>
     * Only the original uploader may delete.
     *
     * @param materialId the material to delete.
     * @param user       the authenticated user.
     * @throws ApiException 404 if the material does not exist.
     * @throws ApiException 403 if the requester is not the uploader.
     */
    @Transactional
    public void deleteMaterial(UUID materialId, User user) {
        Material material = requireMaterial(materialId);

        if (!material.getUploadedBy().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Only the uploader can delete this material");
        }

        // 1. Remove all bookmarks (cascade-clean the material_links table)
        materialLinkRepository.deleteByMaterialId(materialId);

        // 2. Delete the physical file if this is a file material
        if (!RESOURCE_TYPE_LINK.equals(material.getResourceType())) {
            fileStorageService.delete(material.getUrl());
        }

        // 3. Remove the material record
        materialRepository.deleteById(materialId);
        log.debug("Material {} deleted by user {}", materialId, user.getId());
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    /**
     * Returns a single material.
     *
     * @param materialId the material's UUID.
     * @param user       the authenticated user (must be a space member).
     * @return the material as a response DTO, with {@code isBookmarked} populated.
     * @throws ApiException 404 if not found.
     * @throws ApiException 403 if the user is not a member of the material's space.
     */
    @Transactional(readOnly = true)
    public MaterialResponse getMaterial(UUID materialId, User user) {
        Material material = requireMaterial(materialId);
        requireMember(material.getSpace().getId(), user.getId());
        boolean isBookmarked = materialLinkRepository.existsByMaterialIdAndUserId(materialId, user.getId());
        return toResponse(material, isBookmarked);
    }

    /**
     * Returns all materials in a space ordered by creation date (newest first).
     *
     * <p>
     * The {@code isBookmarked} flag on each item reflects the requesting user's
     * bookmark state, so the frontend can render bookmark icons without extra
     * calls.
     *
     * @param spaceId the space to query.
     * @param user    the authenticated user (must be a space member).
     * @return list of materials with bookmark state for the requesting user.
     * @throws ApiException 404 if the space does not exist.
     * @throws ApiException 403 if the user is not a space member.
     */
    @Transactional(readOnly = true)
    public List<MaterialResponse> getMaterialsBySpace(UUID spaceId, User user) {
        requireMemberSpace(spaceId, user.getId());
        UUID userId = user.getId();
        return materialRepository.findBySpaceIdOrderByCreatedAtDesc(spaceId)
                .stream()
                .map(m -> toResponse(m,
                        materialLinkRepository.existsByMaterialIdAndUserId(m.getId(), userId)))
                .toList();
    }

    /**
     * Returns only the materials bookmarked by the requesting user in a specific
     * space.
     *
     * @param spaceId the space to filter by.
     * @param user    the authenticated user (must be a space member).
     * @return list of bookmarked materials, ordered by bookmark date (newest
     *         first).
     * @throws ApiException 404 if the space does not exist.
     * @throws ApiException 403 if the user is not a space member.
     */
    @Transactional(readOnly = true)
    public List<MaterialResponse> getBookmarkedMaterials(UUID spaceId, User user) {
        requireMemberSpace(spaceId, user.getId());
        return materialLinkRepository.findByUserIdAndSpaceId(user.getId(), spaceId)
                .stream()
                .map(link -> toResponse(link.getMaterial(), true)) // isBookmarked always true here
                .toList();
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Loads a material by ID or throws 404.
     */
    private Material requireMaterial(UUID materialId) {
        return materialRepository.findById(materialId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Material not found with id: " + materialId));
    }

    /**
     * Loads a space by ID and verifies the given user is a member.
     *
     * @return the loaded {@link Space} entity for further use.
     * @throws ApiException 404 if the space does not exist.
     * @throws ApiException 403 if the user is not a member.
     */
    private Space requireMemberSpace(UUID spaceId, UUID userId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Space not found with id: " + spaceId));
        requireMember(spaceId, userId);
        return space;
    }

    /**
     * Verifies that the user is a member of the given space.
     *
     * @throws ApiException 403 if the user is not a member.
     */
    private void requireMember(UUID spaceId, UUID userId) {
        if (!spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "You must be a member of this space to access its materials");
        }
    }

    /**
     * Maps a {@link Material} entity to its response DTO.
     *
     * <p>
     * Must be called within an active transaction because {@code space} and
     * {@code uploadedBy} are lazily loaded.
     *
     * @param m            the material entity.
     * @param isBookmarked whether the requesting user has bookmarked this material.
     */
    private MaterialResponse toResponse(Material m, boolean isBookmarked) {
        return MaterialResponse.builder()
                .id(m.getId())
                .spaceId(m.getSpace().getId())
                .spaceName(m.getSpace().getName())
                .uploadedById(m.getUploadedBy().getId())
                .uploadedByName(m.getUploadedBy().getFullName())
                .title(m.getTitle())
                .description(m.getDescription())
                .resourceType(m.getResourceType())
                .url(m.getUrl())
                .fileSizeKb(m.getFileSizeKb())
                .linkCount(m.getLinkCount())
                .isBookmarked(isBookmarked)
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}