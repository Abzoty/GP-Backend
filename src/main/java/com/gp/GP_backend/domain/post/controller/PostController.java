package com.gp.GP_backend.domain.post.controller;

import com.gp.GP_backend.domain.post.dto.CreateAnswerRequest;
import com.gp.GP_backend.domain.post.dto.CreatePostRequest;
import com.gp.GP_backend.domain.post.dto.AnswerResponse;
import com.gp.GP_backend.domain.post.dto.PostResponse;
import com.gp.GP_backend.domain.post.service.PostService;
import com.gp.GP_backend.domain.space.service.SpaceService;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.exception.ApiException;
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

import java.util.UUID;

/**
 * REST controller for Posts and Answers.
 *
 * <p>Base paths:
 * <ul>
 *   <li>{@code POST /api/v1/spaces/{spaceId}/posts} — US-014: create a post</li>
 *   <li>{@code GET  /api/v1/posts/{postId}}          — fetch a single post</li>
 *   <li>{@code POST /api/v1/posts/{postId}/answers}  — US-015: answer a post</li>
 * </ul>
 *
 * <p>The authenticated user is injected via {@code @AuthenticationPrincipal}
 * — Spring Security resolves it from the JWT set by {@code JwtAuthFilter}.
 * Since {@code User} implements {@code UserDetails}, no extra lookup is needed.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Posts & Answers", description = "Create and retrieve posts and answers within spaces")
@SecurityRequirement(name = "bearerAuth")
public class PostController {

    private final PostService postService;
    private final SpaceService spaceService;

    // ─── US-014: Create a post ────────────────────────────────────────────────

    /**
     * Creates a question or discussion post inside a space.
     *
     * <p>The caller must be an authenticated member of the target space.
     * Returns 201 Created with the saved post on success.
     *
     * @param spaceId  path variable — the space to post in
     * @param request  validated JSON body
     * @param author   resolved from JWT — the currently authenticated user
     */
    @PostMapping("/api/v1/spaces/{spaceId}/posts")
    @Operation(summary = "Create a post in a space (US-014)")
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @PathVariable UUID spaceId,
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal User author) {

        PostResponse post = postService.createPost(spaceId, author, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Post created successfully", post));
    }

    // ─── Get a single post ────────────────────────────────────────────────────

    /**
     * Fetches a post by ID and increments its view counter.
     * Any authenticated user can view a post (no membership check here —
     * feed visibility is enforced at the space level).
     */
    @GetMapping("/api/v1/posts/{postId}")
    @Operation(summary = "Get a post by ID")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(@PathVariable UUID postId, @AuthenticationPrincipal User user) {
        UUID spaceId = postService.getSpaceIdForPost(postId);
        boolean isMember = spaceService.isMemberInSpace(spaceId, user.getId());
        if (!isMember) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Post not found");
        }
        PostResponse post = postService.getPost(postId);
        return ResponseEntity.ok(ApiResponse.ok("Post retrieved", post));
    }

    // ─── US-015: Answer a post ────────────────────────────────────────────────

    /**
     * Submits an answer to an existing question post.
     *
     * <p>The caller must be an authenticated member of the space that owns
     * the post. Returns 201 Created with the saved answer on success.
     *
     * @param postId   path variable — the post being answered
     * @param request  validated JSON body
     * @param author   resolved from JWT — the currently authenticated user
     */
    @PostMapping("/api/v1/posts/{postId}/answers")
    @Operation(summary = "Answer a question post (US-015)")
    public ResponseEntity<ApiResponse<AnswerResponse>> createAnswer(
            @PathVariable UUID postId,
            @Valid @RequestBody CreateAnswerRequest request,
            @AuthenticationPrincipal User author) {

        AnswerResponse answer = postService.createAnswer(postId, author, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Answer submitted successfully", answer));
    }
}