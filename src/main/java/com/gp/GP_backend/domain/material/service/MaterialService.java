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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialService {

        private static final String RESOURCE_TYPE_LINK = "LINK";

        /** Valid sort fields for material search. */
        private static final Set<String> MATERIAL_SORT_FIELDS = Set.of("linkCount", "createdAt");

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
try{

        Material saved = materialRepository.save(material);
        notificationService.notifyNewMaterialShared(saved, uploader);

        gamificationService.awardXp(
                uploader.getId(),
                XpCalculator.EVENT_MATERIAL_SHARED,
                XpCalculator.XP_MATERIAL_SHARED,
                saved.getId(),
                XpCalculator.REF_MATERIAL);

        return toResponse(saved, false);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "You have already uploaded a file with the same name in this space");
        }
    }




        // ─── Share link ────────────────────────────────────────────────────────────

       @Transactional
    public MaterialResponse shareLink(UUID spaceId, ShareLinkRequest request, User uploader) {
        Space space = requireMemberSpace(spaceId, uploader.getId());

        Material material = Material.builder()
                .space(space)
                .uploadedBy(uploader)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .resourceType(RESOURCE_TYPE_LINK)
                .url(request.getUrl())
                .fileSizeKb(null) // links have no file size
                .build();
try{

        Material saved = materialRepository.save(material);
        notificationService.notifyNewMaterialShared(saved, uploader);

        gamificationService.awardXp(
                uploader.getId(),
                XpCalculator.EVENT_MATERIAL_SHARED,
                XpCalculator.XP_MATERIAL_SHARED,
                saved.getId(),
                XpCalculator.REF_MATERIAL);
                return toResponse(saved, false);
                } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "You have already shared this link in this space");
        }
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
        try {
            MaterialLink savedLink = materialLinkRepository.save(link);

            materialLinkRepository.incrementLinkCount(materialId);

            // Award the material owner only when another user bookmarks it.
            UUID ownerId = material.getUploadedBy().getId();
            if (!ownerId.equals(user.getId())) {
                // Use the bookmark row ID as reference so multiple different users
                // can bookmark the same material and each award XP once.
                gamificationService.awardXp(
                        ownerId,
                        XpCalculator.EVENT_MATERIAL_LINKED,
                        XpCalculator.XP_MATERIAL_LINKED,
                        savedLink.getId(),
                        XpCalculator.REF_BOOKMARK);
            }
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "You have already bookmarked this material");
        }
    }

        @Transactional
    public void unbookmark(UUID materialId, User user) {
        Material material = requireMaterial(materialId);
        requireMember(material.getSpace().getId(), user.getId());

        MaterialLink link = materialLinkRepository.findByMaterialIdAndUserId(materialId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "You have not bookmarked this material"));

        UUID ownerId = material.getUploadedBy().getId();
        if (!ownerId.equals(user.getId())) {
            gamificationService.revokeXp(
                ownerId,
                XpCalculator.EVENT_MATERIAL_LINKED,
                link.getId(),
                XpCalculator.REF_BOOKMARK);
        }

        materialLinkRepository.delete(link);

        // Decrement the counter, guarding against going below zero
        materialLinkRepository.decrementLinkCount(materialId);
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


    /**
     * Returns all materials in a space ordered by creation date (newest first).
     * The isBookmarked flag on each item reflects the requesting user's bookmark
     * state,
     * so the frontend can render bookmark icons without extra calls.
     */
    @Transactional(readOnly = true)
    public List<MaterialResponse> getMaterialsBySpace(UUID spaceId, User user) {
        requireMemberSpace(spaceId, user.getId());
        Set<UUID> bookmarkedMaterialIds = new HashSet<>(
            materialLinkRepository.findByUserIdAndSpaceId(user.getId(), spaceId)
                .stream()
                .map(link -> link.getMaterial().getId())
                .toList());

        return materialRepository.findBySpaceIdOrderByCreatedAtDesc(spaceId)
                .stream()
            .map(m -> toResponse(m, bookmarkedMaterialIds.contains(m.getId())))
                .toList();
    }

        // CHANGE — add the size cap inside the service as a second line of defence
    @Transactional(readOnly = true)
    public Page<MaterialResponse> getMaterialsBySpacePaged(UUID spaceId, User user, int page, int size) {
        requireMemberSpace(spaceId, user.getId());

        //  ADD — never trust the caller, cap inside the service too
        int safeSize = Math.min(size, 50);

        Set<UUID> bookmarkedMaterialIds = new HashSet<>(
            materialLinkRepository.findByUserIdAndSpaceId(user.getId(), spaceId)
                .stream()
                .map(link -> link.getMaterial().getId())
                .toList());

        //  CHANGE — use safeSize instead of size
        return materialRepository.findBySpaceIdOrderByCreatedAtDesc(spaceId, PageRequest.of(page, safeSize))
            .map(m -> toResponse(m, bookmarkedMaterialIds.contains(m.getId())));
    }

    // Returns only the materials bookmarked by the requesting user in a specific space.
    // Returns only the materials bookmarked by the requesting user in a specific
    // space.
   // CHANGE to paginated
    @Transactional(readOnly = true)
    public Page<MaterialResponse> getBookmarkedMaterials(UUID spaceId, User user, int page, int size) {
        requireMemberSpace(spaceId, user.getId());
        int safeSize = Math.min(size, 50);
        return materialLinkRepository
                .findByUserIdAndSpaceId(user.getId(), spaceId, PageRequest.of(page, safeSize))
                .map(link -> toResponse(link.getMaterial(), true));
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


        /**
         * Searches materials in a space for title/description with optional filters for
         * resource type, plus pagination and sorting. The isBookmarked flag on each item
         * @param spaceId
         * @param query
         * @param resourceType
         * @param sortBy
         * @param sortDir
         * @param page
         * @param size
         * @param user
         * @return
         */

        @Transactional(readOnly = true)
        public List<MaterialResponse> searchMaterials(
                        UUID spaceId, String query, String resourceType,
                        String sortBy, String sortDir, int page, int size, User user) {

                requireMemberSpace(spaceId, user.getId());

                Sort sort = buildSort(sortBy, sortDir);
                PageRequest pageable = PageRequest.of(page, size, sort);

                String normalizedQuery = (query == null || query.isBlank()) ? null : query.trim();
                String normalizedType = (resourceType == null || resourceType.isBlank()) ? null
                                : resourceType.trim().toUpperCase();

                List<Material> materials = materialRepository
                                .searchMaterials(spaceId, normalizedQuery, normalizedType, pageable)
                                .getContent();

                if (materials.isEmpty())
                        return List.of();

                // Fetch all bookmarks for this page in ONE query
                List<UUID> materialIds = materials.stream().map(Material::getId).toList();
                Set<UUID> bookmarkedIds = materialLinkRepository.findBookmarkedMaterialIds(user.getId(), materialIds);

                return materials.stream()
                                .map(m -> toResponse(m, bookmarkedIds.contains(m.getId())))
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

        /**
         * Builds a {@link Sort} from the supplied field name and direction, falling
         * back to {@code createdAt DESC} for unknown values.
         */
        private Sort buildSort(String sortBy, String sortDir) {
                Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                                ? Sort.Direction.ASC
                                : Sort.Direction.DESC;
                String field = (sortBy != null && MATERIAL_SORT_FIELDS.contains(sortBy)) ? sortBy : "createdAt";
                return Sort.by(direction, field);
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
}