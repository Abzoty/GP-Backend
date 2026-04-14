package com.gp.GP_backend.domain.space.controller;

import com.gp.GP_backend.domain.space.dto.CreateSpaceRequest;
import com.gp.GP_backend.domain.space.dto.MembershipResponse;
import com.gp.GP_backend.domain.space.dto.SpaceResponse;
import com.gp.GP_backend.domain.space.dto.UpdateSpaceRequest;
import com.gp.GP_backend.domain.space.service.SpaceService;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.response.ApiResponse;
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

    // ─── GetSpaceById ─────────────────────────────────────────────────────────────────

    @GetMapping("/{spaceId}")
    public ResponseEntity<ApiResponse<?>> getSpace(
            @PathVariable UUID spaceId,
            @AuthenticationPrincipal User currentUser){
                UUID userId= currentUser.getId();
        SpaceResponse space = spaceService.getSpaceById(spaceId, userId);

        if (space != null) {
            return ResponseEntity.ok(ApiResponse.ok("Space retrieved successfully", space));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<SpaceResponse>builder()
                        .success(false)
                        .message("Space not found")
                        .build());
    }


    @GetMapping("all-spaces")
    public ResponseEntity<ApiResponse<?>> getAllSpaces(
            @AuthenticationPrincipal User currentUser){
                UUID userId= currentUser.getId();
        List<SpaceResponse> spaces = spaceService.getSpacesByUserId(userId);

        if (spaces != null && !spaces.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok("Spaces retrieved successfully", spaces));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<SpaceResponse>builder()
                        .success(false)
                        .message("No spaces found for the user")
                        .build());
    }

    @GetMapping("active-spaces")
    public ResponseEntity<ApiResponse<?>> getActiveSpaces(){
        List<SpaceResponse> spaces = spaceService.getAllSpaces();

        if (spaces != null && !spaces.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok("Spaces retrieved successfully", spaces));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<SpaceResponse>builder()
                        .success(false)
                        .message("No Active spaces found")
                        .build());
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
    public ResponseEntity<ApiResponse<MembershipResponse>> grantAdmin(
            @PathVariable UUID spaceId,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal User currentUser) {

        MembershipResponse updated = spaceService.grantAdmin(spaceId, memberId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Admin role granted successfully", updated));
    }
}