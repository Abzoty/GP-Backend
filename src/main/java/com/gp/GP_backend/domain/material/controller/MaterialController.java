package com.gp.GP_backend.domain.material.controller;

import com.gp.GP_backend.domain.material.dto.EditMaterialRequest;
import com.gp.GP_backend.domain.material.dto.MaterialResponse;
import com.gp.GP_backend.domain.material.dto.ShareLinkRequest;
import com.gp.GP_backend.domain.material.entity.AcceptedFileType;
import com.gp.GP_backend.domain.material.service.MaterialService;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.exception.ApiException;
import com.gp.GP_backend.shared.response.ApiResponse;
import com.gp.GP_backend.shared.response.PagedResponse;
import com.gp.GP_backend.shared.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/materials")
@RequiredArgsConstructor
@Tag(name = "Materials", description = "File uploads, link sharing, bookmarks, and retrieval within spaces")
@SecurityRequirement(name = "bearerAuth")
public class MaterialController {

    private final MaterialService materialService;
    private final FileStorageService fileStorageService;

    // ─── Create: file upload ──────────────────────────────────────────────────

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file to a space")
    public ResponseEntity<ApiResponse<MaterialResponse>> uploadFile(
            @RequestParam UUID spaceId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) {

        MaterialResponse response = materialService.uploadFile(spaceId, title, description, file, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("File uploaded successfully", response));
    }

    // ─── Create: share link ────────────────────────────────────────────────────

    @PostMapping("/link")
    @Operation(summary = "Share an external link in a space")
    public ResponseEntity<ApiResponse<MaterialResponse>> shareLink(
            @RequestParam UUID spaceId,
            @Valid @RequestBody ShareLinkRequest request,
            @AuthenticationPrincipal User user) {

        MaterialResponse response = materialService.shareLink(spaceId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Link shared successfully", response));
    }

    // ─── Download file ────────────────────────────────────────────────────────

    @GetMapping("/{materialId}/download")
    @Operation(summary = "Download a file material")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable UUID materialId,
            @AuthenticationPrincipal User user) {

        MaterialResponse material = materialService.getMaterial(materialId, user);

        if ("LINK".equals(material.getResourceType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This material is an external link and cannot be downloaded. Open the URL directly.");
        }

        Resource resource = fileStorageService.loadAsResource(material.getUrl());
        String contentType = resolveContentType(material.getResourceType());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    // ─── Bookmark (add) ────────────────────────────────────────────────────────

    @PostMapping("/{materialId}/bookmark")
    @Operation(summary = "Bookmark a material")
    public ResponseEntity<ApiResponse<Void>> bookmark(
            @PathVariable UUID materialId,
            @AuthenticationPrincipal User user) {

        materialService.bookmark(materialId, user);
        return ResponseEntity.ok(ApiResponse.ok("Material bookmarked successfully", null));
    }

    // ─── Bookmark (remove) ────────────────────────────────────────────────────

    @DeleteMapping("/{materialId}/bookmark")
    @Operation(summary = "Remove a bookmark from a material")
    public ResponseEntity<ApiResponse<Void>> unbookmark(
            @PathVariable UUID materialId,
            @AuthenticationPrincipal User user) {

        materialService.unbookmark(materialId, user);
        return ResponseEntity.ok(ApiResponse.ok("Bookmark removed successfully", null));
    }

    // ─── Edit ─────────────────────────────────────────────────────────────────

    @PatchMapping("/{materialId}")
    @Operation(summary = "Edit a material's title and description")
    public ResponseEntity<ApiResponse<MaterialResponse>> editMaterial(
            @PathVariable UUID materialId,
            @Valid @RequestBody EditMaterialRequest request,
            @AuthenticationPrincipal User user) {

        MaterialResponse response = materialService.editMaterial(materialId, request, user);
        return ResponseEntity.ok(ApiResponse.ok("Material updated successfully", response));
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    @DeleteMapping("/{materialId}")
    @Operation(summary = "Delete a material")
    public ResponseEntity<ApiResponse<Void>> deleteMaterial(
            @PathVariable UUID materialId,
            @AuthenticationPrincipal User user) {

        materialService.deleteMaterial(materialId, user);
        return ResponseEntity.ok(ApiResponse.ok("Material deleted successfully", null));
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    @GetMapping("/{materialId}")
    @Operation(summary = "Get a single material by ID")
    public ResponseEntity<ApiResponse<MaterialResponse>> getMaterial(
            @PathVariable UUID materialId,
            @AuthenticationPrincipal User user) {

        MaterialResponse response = materialService.getMaterial(materialId, user);
        return ResponseEntity.ok(ApiResponse.ok("Material retrieved", response));
    }

    @GetMapping("/space/{spaceId}")
    @Operation(summary = "Get all materials in a space")
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> getMaterialsBySpace(
            @PathVariable UUID spaceId,
            @AuthenticationPrincipal User user) {

        List<MaterialResponse> materials = materialService.getMaterialsBySpace(spaceId, user);
        return ResponseEntity.ok(ApiResponse.ok("Materials retrieved", materials));
    }
    // all materails endpoints with pagination
    @GetMapping("/space/{spaceId}/materials")
    public ResponseEntity<ApiResponse<Page<MaterialResponse>>> getMaterials(
            @PathVariable UUID spaceId,
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        // Cap size so clients can't request 999999 items
        int safeSize = Math.min(size, 50);

        return ResponseEntity.ok(ApiResponse.ok("Materials fetched",
                materialService.getMaterialsBySpacePaged(spaceId, user, page, safeSize)));
    }
    @GetMapping("/space/{spaceId}/paged")
    @Operation(summary = "Get paged materials in a space")
    public ResponseEntity<ApiResponse<PagedResponse<MaterialResponse>>> getMaterialsBySpacePaged(
            @PathVariable UUID spaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {

        if (page < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Page must be >= 0");
        }
        if (size <= 0 || size > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Size must be between 1 and 100");
        }

        PagedResponse<MaterialResponse> data = PagedResponse.of(
                materialService.getMaterialsBySpacePaged(spaceId, user, page, size));

        return ResponseEntity.ok(ApiResponse.ok("Materials retrieved", data));
    }

    @GetMapping("/space/{spaceId}/bookmarked")
    @Operation(summary = "Get bookmarked materials in a space for the current user")
    public ResponseEntity<ApiResponse<Page<MaterialResponse>>> getBookmarkedMaterials(
            @PathVariable UUID spaceId,
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<MaterialResponse> materials = materialService.getBookmarkedMaterials(spaceId, user, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Bookmarked materials retrieved", materials));
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    /**
     * Searches materials within a space by title or description, with optional
     * resource-type filter and configurable sort.
     *
     * <p>
     * Caller must be a member of the space.
     *
     * @param spaceId      the space to search within.
     * @param query        substring matched against title and description.
     * @param resourceType filter by type: {@code "PDF"}, {@code "DOCX"},
     *                     {@code "TXT"}, {@code "MD"}, {@code "DOC"},
     *                     or {@code "LINK"}.
     * @param sortBy       {@code "linkCount"} or {@code "createdAt"} (default).
     * @param sortDir      {@code "asc"} or {@code "desc"} (default).
     * @param page         zero-based page index (default 0).
     * @param size         page size (default 20).
     */
    @GetMapping("/space/{spaceId}/search")
    @Operation(summary = "Search materials in a space by title/description with optional type filter and sort")
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> searchMaterials(
            @PathVariable UUID spaceId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String resourceType,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {

        List<MaterialResponse> results = materialService.searchMaterials(
                spaceId, query, resourceType, sortBy, sortDir, page, size, user);
        return ResponseEntity.ok(ApiResponse.ok("Materials retrieved", results));
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private String resolveContentType(String resourceType) {
        if (resourceType == null)
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        try {
            return AcceptedFileType.valueOf(resourceType).getMimeType();
        } catch (IllegalArgumentException e) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }
}