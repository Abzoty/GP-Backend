package com.gp.GP_backend.domain.post.controller;

import com.gp.GP_backend.domain.post.dto.CreateAnswerRequest;
import com.gp.GP_backend.domain.post.dto.CreatePostRequest;
import com.gp.GP_backend.domain.post.dto.EditAnswerRequest;
import com.gp.GP_backend.domain.post.dto.EditPostRequest;
import com.gp.GP_backend.domain.post.dto.AllPostsResponse;
import com.gp.GP_backend.domain.post.dto.AnswerResponse;
import com.gp.GP_backend.domain.post.dto.PostResponse;
import com.gp.GP_backend.domain.post.service.PostService;
import com.gp.GP_backend.domain.post.service.VoteService;
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

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Posts and Answers.
 *
 * <p>
 * Base paths:
 * <ul>
 * <li>{@code POST /api/v1/spaces/{spaceId}/posts} — US-014: create a post</li>
 * <li>{@code GET  /api/v1/posts/{postId}} — fetch a single post</li>
 * <li>{@code POST /api/v1/posts/{postId}/answers} — US-015: answer a post</li>
 * </ul>
 *
 * <p>
 * The authenticated user is injected via {@code @AuthenticationPrincipal}
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
    private final VoteService voteService;

    // ─── US-014: Create a post ────────────────────────────────────────────────

    /**
     * Creates a question or discussion post inside a space.
     *
     * <p>
     * The caller must be an authenticated member of the target space.
     * Returns 201 Created with the saved post on success.
     *
     * @param spaceId path variable — the space to post in
     * @param request validated JSON body
     * @param author  resolved from JWT — the currently authenticated user
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

    @PostMapping("/api/v1/spaces/posts/{postId}")
    @Operation(summary = "mark post as good question (US-017)")
    public ResponseEntity<ApiResponse<?>> goodQuestionPost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal User user) {
        boolean isDone = voteService.markGoodQuestion(postId, user);
        if (!isDone) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unable to mark post as good question");
        }
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.ok("Post marked as good question", null));
    }

    @PutMapping("/api/v1/spaces/posts/{postId}")
    @Operation(summary = "Edit an existing post")
    public ResponseEntity<ApiResponse<Boolean>> editPost(
            @PathVariable UUID postId,
            @Valid @RequestBody EditPostRequest request,
            @AuthenticationPrincipal User user) {
        boolean isEdited = postService.editPost(request, user.getId(), postId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.ok("Post edited successfully", isEdited));
    }

    @DeleteMapping("/api/v1/spaces/posts/{postId}")
    @Operation(summary = "Delete an existing post")
    public ResponseEntity<ApiResponse<Boolean>> deletePost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal User user) {
        boolean isDeleted = postService.deletePost(postId, user.getId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.ok("Post deleted successfully", isDeleted));
    }

    @PostMapping("/api/v1/spaces/posts/{postId}/answers/{answerId}")
    @Operation(summary = "mark post as solved (US-018)")
    public ResponseEntity<ApiResponse<?>> markPostAsSolved(
            @PathVariable UUID postId,
            @PathVariable UUID answerId,
            @AuthenticationPrincipal User user) {
        boolean isDone = postService.markQuestionAsSolved(postId, answerId, user);
        if (!isDone) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unable to mark post as solved");
        }
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.ok("Post marked as solved", null));
    }

    @PostMapping("/api/v1/spaces/posts/answers/{answerId}")
    @Operation(summary = "upvote answer (US-016)")
    public ResponseEntity<ApiResponse<?>> upvoteAnswer(
            @PathVariable UUID answerId,
            @AuthenticationPrincipal User user) {
        boolean isDone = voteService.upvoteGoodAnswer(answerId, user);
        if (!isDone) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unable to upvote answer");
        }
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.ok("Answer upvoted successfully", null));
    }

    // ─── Get a single post ────────────────────────────────────────────────────

    /**
     * Fetches a post by ID and increments its view counter.
     * Any authenticated user can view a post (no membership check here —
     * feed visibility is enforced at the space level).
     */
    @GetMapping("/api/v1/posts/{postId}")
    @Operation(summary = "Get a post by ID")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal User user) {
        UUID spaceId = postService.getSpaceIdForPost(postId);
        boolean isMember = spaceService.isMemberInSpace(spaceId, user.getId());
        if (!isMember) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Post not found");
        }
        PostResponse post = postService.getPost(postId);
        return ResponseEntity.ok(ApiResponse.ok("Post retrieved", post));
    }

    @GetMapping("/api/v1/posts/{postId}/answers/{page}/{size}")
    @Operation(summary = "Get all post answers")
    public ResponseEntity<ApiResponse<List<AnswerResponse>>> getPostAnswers(
            @PathVariable UUID postId,
            @AuthenticationPrincipal User user,
            @PathVariable int page,
            @PathVariable int size) {
        return ResponseEntity.ok(ApiResponse.ok("Post Answers retrieved",
                postService.getPostAnswers(postId, user.getId(), page, size)));
    }

    @GetMapping("/api/v1/posts/all-posts/{spaceId}/{page}/{size}")
    @Operation(summary = "Get all posts of space ordered by creation date")
    public ResponseEntity<ApiResponse<List<AllPostsResponse>>> allPosts(
            @PathVariable UUID spaceId,
            @AuthenticationPrincipal User user,
            @PathVariable int page,
            @PathVariable int size) {
        List<AllPostsResponse> posts = postService.getAllPost(user.getId(), spaceId, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Posts retrieved", posts));
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    /**
     * Searches posts within a space by title or body, with optional solved filter
     * and configurable sort.
     *
     * <p>
     * Caller must be a member of the space.
     *
     * @param spaceId  the space to search within.
     * @param query    substring matched against title and body.
     * @param isSolved {@code true} = only solved, {@code false} = only unsolved,
     *                 omit = all posts.
     * @param sortBy   {@code "goodQuestionCount"} or {@code "createdAt"} (default).
     * @param sortDir  {@code "asc"} or {@code "desc"} (default).
     * @param page     zero-based page index (default 0).
     * @param size     page size (default 20).
     */
    @GetMapping("/api/v1/posts/search/{spaceId}")
    @Operation(summary = "Search posts in a space by title/body with optional solved filter and sort")
    public ResponseEntity<ApiResponse<List<AllPostsResponse>>> searchPosts(
            @PathVariable UUID spaceId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean isSolved,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {

        List<AllPostsResponse> results = postService.searchPosts(
                user.getId(), spaceId, query, isSolved, sortBy, sortDir, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Posts retrieved", results));
    }

    // ─── US-015: Answer a post ────────────────────────────────────────────────

    /**
     * Submits an answer to an existing question post.
     *
     * <p>
     * The caller must be an authenticated member of the space that owns
     * the post. Returns 201 Created with the saved answer on success.
     *
     * @param postId  path variable — the post being answered
     * @param request validated JSON body
     * @param author  resolved from JWT — the currently authenticated user
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

    @PutMapping("/api/v1/answers/{answerId}")
    @Operation(summary = "Edit an existing answer")
    public ResponseEntity<ApiResponse<Boolean>> editAnswer(
            @PathVariable UUID answerId,
            @Valid @RequestBody EditAnswerRequest request,
            @AuthenticationPrincipal User user) {

        boolean isEdited = postService.editAnswer(request, user.getId(), answerId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.ok("Answer edited successfully", isEdited));
    }

    @DeleteMapping("/api/v1/answers/{answerId}")
    @Operation(summary = "Delete an existing answer")
    public ResponseEntity<ApiResponse<Boolean>> deleteAnswer(
            @PathVariable UUID answerId,
            @AuthenticationPrincipal User user) {

        boolean isDeleted = postService.deleteAnswer(answerId, user.getId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.ok("Answer deleted successfully", isDeleted));
    }

}