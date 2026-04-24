package com.gp.GP_backend.domain.material.service;

import com.gp.GP_backend.domain.material.dto.EditMaterialRequest;
import com.gp.GP_backend.domain.material.dto.MaterialResponse;
import com.gp.GP_backend.domain.material.dto.ShareLinkRequest;
import com.gp.GP_backend.domain.material.entity.AcceptedFileType;
import com.gp.GP_backend.domain.material.entity.Material;
import com.gp.GP_backend.domain.material.entity.MaterialLink;
import com.gp.GP_backend.domain.material.repository.MaterialLinkRepository;
import com.gp.GP_backend.domain.material.repository.MaterialRepository;
import com.gp.GP_backend.domain.notification.service.NotificationService;
import com.gp.GP_backend.domain.space.entity.Space;
import com.gp.GP_backend.domain.space.repository.SpaceMembershipRepository;
import com.gp.GP_backend.domain.space.repository.SpaceRepository;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.service.GamificationService;
import com.gp.GP_backend.shared.exception.ApiException;
import com.gp.GP_backend.shared.storage.FileStorageService;
import com.gp.GP_backend.shared.util.XpCalculator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialService {

    private static final String RESOURCE_TYPE_LINK = "LINK";

    private final MaterialRepository materialRepository;
    private final MaterialLinkRepository materialLinkRepository;
    private final SpaceMembershipRepository spaceMembershipRepository;
    private final SpaceRepository spaceRepository;
    private final FileStorageService fileStorageService;
    private final GamificationService gamificationService;
    private final NotificationService notificationService;

    // ─── Upload file ──────────────────────────────────────────────────────────

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
        notificationService.notifyNewMaterialShared(saved, uploader);

        gamificationService.awardXp(
                uploader.getId(),
                XpCalculator.EVENT_MATERIAL_SHARED,
                XpCalculator.XP_MATERIAL_SHARED,
                saved.getId(),
                XpCalculator.REF_MATERIAL);

        return toResponse(saved, false);
    }

    // ─── Share link ────────────────────────────────────────────────────────────

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
        notificationService.notifyNewMaterialShared(saved, uploader);

        gamificationService.awardXp(
                uploader.getId(),
                XpCalculator.EVENT_MATERIAL_SHARED,
                XpCalculator.XP_MATERIAL_SHARED,
                saved.getId(),
                XpCalculator.REF_MATERIAL);

        return toResponse(saved, false);
    }

    // ─── Bookmark (add) ────────────────────────────────────────────────────────

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

        // Increment the denormalized bookmark counter
        material.setLinkCount(material.getLinkCount() + 1);
        materialRepository.save(material);

        // Award the material's owner, not the person bookmarking
        gamificationService.awardXp(
                material.getUploadedBy().getId(),
                XpCalculator.EVENT_MATERIAL_LINKED,
                XpCalculator.XP_MATERIAL_LINKED,
                materialId,
                XpCalculator.REF_MATERIAL);
    }

    // ─── Bookmark (remove) ────────────────────────────────────────────────────

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
    }

    // ─── Edit ─────────────────────────────────────────────────────────────────

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

        return toResponse(saved, isBookmarked);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

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
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public MaterialResponse getMaterial(UUID materialId, User user) {
        Material material = requireMaterial(materialId);
        requireMember(material.getSpace().getId(), user.getId());
        boolean isBookmarked = materialLinkRepository.existsByMaterialIdAndUserId(materialId, user.getId());
        return toResponse(material, isBookmarked);
    }

    /**
     * Returns all materials in a space ordered by creation date (newest first).
     * The isBookmarked flag on each item reflects the requesting user's bookmark
     * state,
     * so the frontend can render bookmark icons without extra calls.
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

    // Returns only the materials bookmarked by the requesting user in a specific
    // space.
    @Transactional(readOnly = true)
    public List<MaterialResponse> getBookmarkedMaterials(UUID spaceId, User user) {
        requireMemberSpace(spaceId, user.getId());
        return materialLinkRepository.findByUserIdAndSpaceId(user.getId(), spaceId)
                .stream()
                .map(link -> toResponse(link.getMaterial(), true)) // isBookmarked always true here
                .toList();
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    // Loads a material by ID or throws 404.
    private Material requireMaterial(UUID materialId) {
        return materialRepository.findById(materialId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Material not found with id: " + materialId));
    }

    // Loads a space by ID and verifies the given user is a member.
    private Space requireMemberSpace(UUID spaceId, UUID userId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Space not found with id: " + spaceId));
        requireMember(spaceId, userId);
        return space;
    }

    // Verifies that the user is a member of the given space.
    private void requireMember(UUID spaceId, UUID userId) {
        if (!spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "You must be a member of this space to access its materials");
        }
    }

    // Maps a Material entity to its response DTO.
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