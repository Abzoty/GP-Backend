package com.gp.GP_backend.domain.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Business logic for posts: creation, viewing, accepting answers, etc.
 *
 * TODO: Implement:
 * - createPost(CreatePostRequest, User author) — saves post, awards
 * XP_POST_CREATED
 * - getPostsBySpace(UUID spaceId, Pageable) — returns paginated PostResponse
 * - getPostById(UUID postId) — increments view_count, returns PostResponse with
 * answers
 * - acceptAnswer(UUID postId, UUID answerId, User requester) — sets
 * accepted_answer, marks solved
 * - deletePost(UUID postId, User requester) — soft-delete or hard-delete with
 * auth check
 */
@Service
@RequiredArgsConstructor
public class PostService {
    // TODO: inject PostRepository, AnswerRepository, GamificationService,
    // UserService
}
