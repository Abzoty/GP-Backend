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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/spaces")
@RequiredArgsConstructor
@Tag(name = "Spaces", description = "Space management, membership, and discovery")
@SecurityRequirement(name = "bearerAuth")
public class SpaceController {

    private final SpaceService spaceService;

    // ─── Create ───────────────────────────────────────────────────────────────

    /**
     * Creates a new space.
     *
     * <p>
     * When {@code force=0} (the default), a similarity/duplicate check is performed
     * before creation. If conflicts are found, the endpoint returns {@code 409}
     * with
     * the list of similar spaces in the response body — the client can then
     * re-submit
     * with {@code force=1} to bypass the check.
     *
     * <p>
     * On success, the creating user is automatically enrolled as an {@code ADMIN}
     * member.
     *
     * @param request     the space creation payload.
     * @param force       {@code 0} = check for conflicts first; {@code 1} = create
     *                    immediately.
     * @param currentUser the authenticated creator.
     * @return {@code 201 CREATED} with the new space, or {@code 409 CONFLICT} with
     *         similar spaces.
     */
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
    public ResponseEntity<ApiResponse<?>> getSpace(
            @PathVariable UUID spaceId,
            @AuthenticationPrincipal User currentUser) {
        UUID userId = currentUser.getId();
        SpaceResponse space = spaceService.getSpaceById(spaceId, userId);
        return ResponseEntity.ok(ApiResponse.ok("Space retrieved successfully", space));
    }

    @GetMapping("all-spaces")
    @Operation(summary = "Retrieve all spaces for the authenticated user")
    public ResponseEntity<ApiResponse<?>> getAllSpaces(
            @AuthenticationPrincipal User currentUser) {
        UUID userId = currentUser.getId();
        List<SpaceResponse> spaces = spaceService.getSpacesByUserId(userId);
        return ResponseEntity.ok(ApiResponse.ok("Spaces retrieved successfully", spaces));
    }

    @GetMapping("active-spaces")
    @Operation(summary = "Retrieve all active spaces")
    public ResponseEntity<ApiResponse<?>> getActiveSpaces() {
        List<SpaceResponse> spaces = spaceService.getAllSpaces();
        return ResponseEntity.ok(ApiResponse.ok("Spaces retrieved successfully", spaces));
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    /**
     * Searches all active spaces with optional text, category filter, and sort.
     *
     * <p>
     * All parameters are optional. Omitting them returns all active spaces
     * sorted by {@code createdAt} descending.
     *
     * @param query    substring matched against name and description.
     * @param category filter by {@link SpaceCategory} enum value.
     * @param sortBy   {@code "memberCount"} or {@code "createdAt"} (default).
     * @param sortDir  {@code "asc"} or {@code "desc"} (default).
     * @param page     zero-based page index (default 0).
     * @param size     page size (default 20).
     */
    @GetMapping("search")
    @Operation(summary = "Search active spaces by name/description with optional category filter and sort")
    public ResponseEntity<ApiResponse<List<SpaceResponse>>> searchSpaces(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) SpaceCategory category,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<SpaceResponse> results = spaceService.searchSpaces(query, category, sortBy, sortDir, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Spaces retrieved", results));
    }

    // ─── Join ─────────────────────────────────────────────────────────────────

    /**
     * Joins the authenticated user to a space as a {@code MEMBER}.
     *
     * @param spaceId     the UUID of the target space.
     * @param currentUser the authenticated user.
     * @return {@code 200 OK} with the created membership record.
     */
    @PostMapping("/{spaceId}/join")
    @Operation(summary = "Join a space as a member")
    public ResponseEntity<ApiResponse<MembershipResponse>> joinSpace(
            @PathVariable UUID spaceId,
            @AuthenticationPrincipal User currentUser) {

        MembershipResponse membership = spaceService.joinSpace(spaceId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Successfully joined the space", membership));
    }

    // ─── Leave ────────────────────────────────────────────────────────────────

    /**
     * Removes the authenticated user from a space.
     *
     * <p>
     * If the user is the <em>sole</em> admin of the space, the request is rejected
     * with {@code 400 BAD REQUEST}. The user must first use the
     * {@code POST /spaces/{spaceId}/admins/{memberId}} endpoint to grant the admin
     * role to another member.
     *
     * @param spaceId     the UUID of the space to leave.
     * @param currentUser the authenticated user.
     * @return {@code 200 OK} on success.
     */
    @DeleteMapping("/{spaceId}/leave")
    @Operation(summary = "Leave a space. If the user is the sole admin, they must grant admin to another member before leaving.")
    public ResponseEntity<ApiResponse<Void>> leaveSpace(
            @PathVariable UUID spaceId,
            @AuthenticationPrincipal User currentUser) {

        spaceService.leaveSpace(spaceId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Successfully left the space", null));
    }

    // ─── Edit ─────────────────────────────────────────────────────────────────

    /**
     * Partially updates the editable fields of a space.
     *
     * <p>
     * Requires the {@code ADMIN} role within the target space.
     * Null fields in the request body are ignored (PATCH semantics).
     *
     * @param spaceId     the UUID of the space to update.
     * @param request     the partial update payload.
     * @param currentUser the authenticated user performing the update.
     * @return {@code 200 OK} with the updated space, or {@code 403 FORBIDDEN}.
     */
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

    /**
     * Grants the {@code ADMIN} role to an existing space member.
     *
     * <p>
     * Only a current {@code ADMIN} of the space may call this endpoint.
     * This is a <em>grant</em> (not a transfer): the requester keeps their own
     * admin role. To step down after granting, call the leave endpoint.
     *
     * @param spaceId     the UUID of the space.
     * @param memberId    the UUID of the member to promote.
     * @param currentUser the authenticated admin performing the grant.
     * @return {@code 200 OK} with the updated membership record.
     */
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