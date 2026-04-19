package com.gp.GP_backend.domain.material.controller;

import com.gp.GP_backend.domain.material.dto.EditMaterialRequest;
import com.gp.GP_backend.domain.material.dto.MaterialResponse;
import com.gp.GP_backend.domain.material.dto.ShareLinkRequest;
import com.gp.GP_backend.domain.material.entity.AcceptedFileType;
import com.gp.GP_backend.domain.material.service.MaterialService;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.exception.ApiException;
import com.gp.GP_backend.shared.response.ApiResponse;
import com.gp.GP_backend.shared.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Materials — file uploads, link sharing, bookmarks,
 * editing, deletion, and retrieval.
 *
 * <p>
 * All endpoints require a valid JWT. Space membership is enforced at the
 * service layer for every operation.
 *
 * <p>
 * Base path: {@code /api/v1/materials}
 *
 * <p>
 * Two creation variants:
 * <ul>
 * <li>{@code POST /upload} — multipart file upload</li>
 * <li>{@code POST /link} — JSON body with an external URL</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/materials")
@RequiredArgsConstructor
@Tag(name = "Materials", description = "File uploads, link sharing, bookmarks, and retrieval within spaces")
@SecurityRequirement(name = "bearerAuth")
public class MaterialController {

    private final MaterialService materialService;
    private final FileStorageService fileStorageService;

    // ─── Create: file upload ──────────────────────────────────────────────────

    /**
     * Uploads a file and registers it as a material in the given space.
     *
     * <p>
     * The request must be {@code multipart/form-data} with the following parts:
     * <ul>
     * <li>{@code spaceId} — UUID of the target space (form field)</li>
     * <li>{@code title} — display title (form field)</li>
     * <li>{@code description} — optional description (form field)</li>
     * <li>{@code file} — the file binary</li>
     * </ul>
     *
     * <p>
     * Accepted file types and the maximum size are defined centrally in
     * {@link AcceptedFileType} and {@link FileStorageService#MAX_FILE_SIZE_BYTES}.
     */
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

    /**
     * Shares an external URL (e.g. a YouTube lecture or online document) as a
     * material inside a space.
     */
    @PostMapping("/link")
    @Operation(summary = "Share an external link in a space")
    public ResponseEntity<ApiResponse<MaterialResponse>> shareLink(
            @Valid @RequestBody ShareLinkRequest request,
            @AuthenticationPrincipal User user) {

        MaterialResponse response = materialService.shareLink(request, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Link shared successfully", response));
    }

    // ─── Download file ────────────────────────────────────────────────────────

    /**
     * Streams a file material back to the client as an attachment.
     *
     * <p>
     * Only works for file-based materials (not links). The requester must be
     * a member of the material's space.
     */
    @GetMapping("/{materialId}/download")
    @Operation(summary = "Download a file material")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable UUID materialId,
            @AuthenticationPrincipal User user) {

        // getMaterial validates membership; throws 404 / 403 as appropriate
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

    /**
     * Bookmarks a material for the authenticated user.
     * Increments the material's bookmark counter ({@code linkCount}).
     */
    @PostMapping("/{materialId}/bookmark")
    @Operation(summary = "Bookmark a material")
    public ResponseEntity<ApiResponse<Void>> bookmark(
            @PathVariable UUID materialId,
            @AuthenticationPrincipal User user) {

        materialService.bookmark(materialId, user);
        return ResponseEntity.ok(ApiResponse.ok("Material bookmarked successfully", null));
    }

    // ─── Bookmark (remove) ────────────────────────────────────────────────────

    /**
     * Removes the authenticated user's bookmark from a material.
     * Decrements the material's bookmark counter ({@code linkCount}).
     */
    @DeleteMapping("/{materialId}/bookmark")
    @Operation(summary = "Remove a bookmark from a material")
    public ResponseEntity<ApiResponse<Void>> unbookmark(
            @PathVariable UUID materialId,
            @AuthenticationPrincipal User user) {

        materialService.unbookmark(materialId, user);
        return ResponseEntity.ok(ApiResponse.ok("Bookmark removed successfully", null));
    }

    // ─── Edit ─────────────────────────────────────────────────────────────────

    /**
     * Updates the title and/or description of a material.
     * Only the original uploader may edit. Null fields are ignored (PATCH
     * semantics).
     */
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

    /**
     * Deletes a material. Only the original uploader may delete.
     *
     * <p>
     * Cascade behaviour:
     * <ul>
     * <li>All bookmarks ({@code material_links} rows) are removed.</li>
     * <li>If the material is a file, the file is deleted from disk.</li>
     * <li>The material record is removed from the database.</li>
     * </ul>
     */
    @DeleteMapping("/{materialId}")
    @Operation(summary = "Delete a material")
    public ResponseEntity<ApiResponse<Void>> deleteMaterial(
            @PathVariable UUID materialId,
            @AuthenticationPrincipal User user) {

        materialService.deleteMaterial(materialId, user);
        return ResponseEntity.ok(ApiResponse.ok("Material deleted successfully", null));
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    /**
     * Returns a single material. The requester must be a member of the
     * material's space.
     */
    @GetMapping("/{materialId}")
    @Operation(summary = "Get a single material by ID")
    public ResponseEntity<ApiResponse<MaterialResponse>> getMaterial(
            @PathVariable UUID materialId,
            @AuthenticationPrincipal User user) {

        MaterialResponse response = materialService.getMaterial(materialId, user);
        return ResponseEntity.ok(ApiResponse.ok("Material retrieved", response));
    }

    /**
     * Returns all materials in a space (files and links), ordered by creation
     * date (newest first). The requester must be a member of the space.
     *
     * <p>
     * Each item includes an {@code isBookmarked} flag for the requesting user,
     * so the frontend can render bookmark icons without extra calls.
     */
    @GetMapping("/space/{spaceId}")
    @Operation(summary = "Get all materials in a space")
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> getMaterialsBySpace(
            @PathVariable UUID spaceId,
            @AuthenticationPrincipal User user) {

        List<MaterialResponse> materials = materialService.getMaterialsBySpace(spaceId, user);
        return ResponseEntity.ok(ApiResponse.ok("Materials retrieved", materials));
    }

    /**
     * Returns only the materials bookmarked by the authenticated user in a
     * specific space, ordered by bookmark date (newest first).
     * The requester must be a member of the space.
     */
    @GetMapping("/space/{spaceId}/bookmarked")
    @Operation(summary = "Get bookmarked materials in a space for the current user")
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> getBookmarkedMaterials(
            @PathVariable UUID spaceId,
            @AuthenticationPrincipal User user) {

        List<MaterialResponse> materials = materialService.getBookmarkedMaterials(spaceId, user);
        return ResponseEntity.ok(ApiResponse.ok("Bookmarked materials retrieved", materials));
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Resolves the HTTP Content-Type header value from a stored resource type.
     *
     * <p>
     * Since resource types are persisted as {@link AcceptedFileType} enum names
     * (e.g. {@code "PDF"}), this simply calls {@code AcceptedFileType.valueOf()}.
     * Falls back to {@code application/octet-stream} for unknown values.
     *
     * @param resourceType the value stored in {@code Material.resourceType}.
     * @return a MIME type string suitable for the {@code Content-Type} header.
     */
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