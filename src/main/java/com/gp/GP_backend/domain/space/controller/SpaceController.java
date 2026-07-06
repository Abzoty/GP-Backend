package com.gp.GP_backend.domain.space.controller;

import com.gp.GP_backend.domain.space.dto.CreateSpaceRequest;
import com.gp.GP_backend.domain.space.dto.MembershipResponse;
import com.gp.GP_backend.domain.space.dto.SpaceResponse;
import com.gp.GP_backend.domain.space.dto.UpdateSpaceRequest;
import com.gp.GP_backend.domain.space.entity.SpaceCategory;
import com.gp.GP_backend.domain.space.service.SpaceService;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/spaces")
@RequiredArgsConstructor
@Tag(name = "Spaces", description = "Space management, membership, and discovery")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class SpaceController {

    private final SpaceService spaceService;

    // ─── Create ───────────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createSpace(
            @Valid @RequestBody CreateSpaceRequest request,
            @RequestParam(defaultValue = "0") int force,
            @AuthenticationPrincipal User currentUser) {

        if (force == 0) {
            List<SpaceResponse> conflicts = spaceService.checkSimilarity(request);

            if (!conflicts.isEmpty()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.<List<SpaceResponse>>builder()
                                .success(false)
                                .message("Similar or duplicate spaces already exist. "
                                        + "Review the list and re-submit with force=1 to create anyway.")
                                .data(conflicts)
                                .build());
            }
        }

        SpaceResponse created = spaceService.createSpace(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Space created successfully", created));
    }

    // ─── GetSpaceById ─────────────────────────────────────────────────────────

    @GetMapping("/{spaceId}")
    @Operation(summary = "Retrieve a space by its UUID")
    public ResponseEntity<ApiResponse<SpaceResponse>> getSpace(
            @PathVariable UUID spaceId,
            @AuthenticationPrincipal User currentUser) {
        UUID userId = currentUser.getId();
        SpaceResponse space = spaceService.getSpaceById(spaceId, userId);
        return ResponseEntity.ok(ApiResponse.ok("Space retrieved successfully", space));
    }

    @GetMapping("/all-spaces")
    @Operation(summary = "Retrieve all spaces for the authenticated user")
    public ResponseEntity<ApiResponse<List<SpaceResponse>>> getAllSpaces(
            @AuthenticationPrincipal User currentUser) {
        UUID userId = currentUser.getId();
        List<SpaceResponse> spaces = spaceService.getSpacesByUserId(userId);
        return ResponseEntity.ok(ApiResponse.ok("Spaces retrieved successfully", spaces));
    }

    @GetMapping("/active-spaces")
    @Operation(summary = "Retrieve active spaces (paginated)")
    public ResponseEntity<ApiResponse<List<SpaceResponse>>> getActiveSpaces(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "0")  @Min(0)            int page) {

        // Pagination is required to avoid full table scans / large JSON payloads in production.
        List<SpaceResponse> spaces = spaceService.getActiveSpaces(page, size);
        return ResponseEntity.ok(ApiResponse.ok("Spaces retrieved successfully", spaces));
    }

    @GetMapping("/{spaceId}/members")
    @Operation(summary = "Retrieve members of a space")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getSpaceMembers(
            @PathVariable UUID spaceId,
            @AuthenticationPrincipal User currentUser) {
        List<MembershipResponse> members = spaceService.getSpaceMembers(spaceId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("Space members retrieved successfully", members));
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    @GetMapping("/search")
    @Operation(summary = "Search active spaces by name/description with optional category filter and sort")
    public ResponseEntity<ApiResponse<List<SpaceResponse>>> searchSpaces(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) SpaceCategory category,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "0")  @Min(0)            int page) {

        List<SpaceResponse> results = spaceService.searchSpaces(query, category, sortBy, sortDir, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Spaces retrieved", results));
    }

    // ─── Join ─────────────────────────────────────────────────────────────────

    @PostMapping("/{spaceId}/join")
    @Operation(summary = "Join a space as a member")
    public ResponseEntity<ApiResponse<MembershipResponse>> joinSpace(
            @PathVariable UUID spaceId,
            @AuthenticationPrincipal User currentUser) {

        MembershipResponse membership = spaceService.joinSpace(spaceId, currentUser);

        // A membership resource was created; use 201 Created + Location pointing to the space.
        URI location = URI.create("/api/v1/spaces/" + spaceId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .header(HttpHeaders.LOCATION, location.toString())
            .body(ApiResponse.ok("Successfully joined the space", membership));
    }

    // ─── Leave ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{spaceId}/leave")
    @Operation(summary = "Leave a space. If the user is the sole admin, they must grant admin to another member before leaving.")
    public ResponseEntity<ApiResponse<Void>> leaveSpace(
            @PathVariable UUID spaceId,
            @AuthenticationPrincipal User currentUser) {

        spaceService.leaveSpace(spaceId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Successfully left the space", null));
    }

    // ─── Edit ─────────────────────────────────────────────────────────────────

    @PatchMapping("/{spaceId}")
    @Operation(summary = "Partially update a space's editable fields (ADMIN only)")
    public ResponseEntity<ApiResponse<SpaceResponse>> updateSpace(
            @PathVariable UUID spaceId,
            @Valid @RequestBody UpdateSpaceRequest request,
            @AuthenticationPrincipal User currentUser) {

        SpaceResponse updated = spaceService.updateSpace(spaceId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Space updated successfully", updated));
    }

    // ─── Grant Admin ──────────────────────────────────────────────────────────

    @PostMapping("/{spaceId}/admins/{memberId}")
    @Operation(summary = "Grant ADMIN role to an existing member of the space")
    public ResponseEntity<ApiResponse<MembershipResponse>> grantAdmin(
            @PathVariable UUID spaceId,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal User currentUser) {

        MembershipResponse updated = spaceService.grantAdmin(spaceId, memberId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Admin role granted successfully", updated));
    }
}