package com.gp.GP_backend.domain.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Handles upvoting of answers and "good question" votes on posts.
 *
 * TODO: Implement:
 * - voteOnAnswer(UUID answerId, User voter) — creates Vote, increments
 * upvote_count, awards XP to answer author
 * - voteOnPost(UUID postId, User voter) — creates Vote, increments
 * good_question_count, awards XP to post author
 * - removeVote(UUID targetId, String targetType, User voter) — reverses XP and
 * counter
 *
 * Each method should check for duplicate votes (a user can only vote once per
 * target).
 */
@Service
@RequiredArgsConstructor
public class VoteService {
    // TODO: inject VoteRepository, PostRepository, AnswerRepository,
    // GamificationService
}
