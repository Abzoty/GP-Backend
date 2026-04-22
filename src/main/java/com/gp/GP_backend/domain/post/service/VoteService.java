package com.gp.GP_backend.domain.post.service;

import com.gp.GP_backend.domain.post.repository.VoteRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.gp.GP_backend.domain.post.entity.Vote;
import com.gp.GP_backend.domain.post.entity.VoteType;
import com.gp.GP_backend.domain.post.entity.TargetType;
import com.gp.GP_backend.domain.post.entity.Answer;
import com.gp.GP_backend.domain.post.repository.AnswerRepository;
import com.gp.GP_backend.domain.post.repository.PostRepository;
import com.gp.GP_backend.domain.space.entity.Space;
import com.gp.GP_backend.domain.space.repository.SpaceMembershipRepository;
import com.gp.GP_backend.domain.space.repository.SpaceRepository;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.exception.ApiException;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.transaction.Transactional;

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

        private final VoteRepository voteRepository;
        private final PostRepository postRepository;
        private final AnswerRepository answerRepository;
        private final SpaceRepository spaceRepository;
        private final SpaceMembershipRepository spaceMembershipRepository;
        // private final GamificationService gamificationService;


        
        @Transactional
        public boolean markGoodQuestion(UUID postId, User user) {
                UUID spaceId = postRepository.findSpaceIdByPostId(postId);
                Space space = spaceRepository.findById(spaceId)
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Space not found"));
                if (!space.getIsActive()) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot vote in an inactive space");
                }
                boolean isMember = spaceMembershipRepository
                                .existsBySpaceIdAndUserId(spaceId, user.getId());
                if (!isMember) {
                        throw new ApiException(HttpStatus.FORBIDDEN,
                "User must be a member of the space to vote");
                }
                UUID authorId = postRepository.findAuthorIdByPostId(postId);
                if (authorId.equals(user.getId())) {
                        throw new ApiException(HttpStatus.BAD_REQUEST,
                "Author cannot vote on their own post");
                }
                if (voteRepository.existsByTargetIdAndTargetTypeAndUserId(postId, TargetType.QUESTION, user.getId())) {
                        throw new ApiException(HttpStatus.CONFLICT, "User has already voted on this question");
                }
                postRepository.incrementGoodQuestionCount(postId);
                Vote newVote = Vote.builder()
                                .targetType(TargetType.QUESTION)
                                .targetId(postId)
                                .user(user)
                                .voteType(VoteType.GOOD_QUESTION)
                                .createdAt(LocalDateTime.now())
                                .build();
                try {
                        voteRepository.save(newVote);
                } catch (DataIntegrityViolationException ex) {
                        throw new ApiException(HttpStatus.CONFLICT, "User has already voted on this question");
                }
                return true;
        }

        @Transactional
        public boolean upvoteGoodAnswer(UUID answerId, User user){
                Answer answer = answerRepository.findById(answerId)
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Answer not found"));
                UUID postId = answer.getPostId();
                UUID spaceId = postRepository.findSpaceIdByPostId(postId);
                Space space = spaceRepository.findById(spaceId)
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Space not found"));
                if (!space.getIsActive()) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot vote in an inactive space");
                }
                boolean isMember = spaceMembershipRepository
                                .existsBySpaceIdAndUserId(spaceId, user.getId());
                if (!isMember) {
                        throw new ApiException(HttpStatus.FORBIDDEN,
                "User must be a member of the space to vote");
                }
                UUID authorId = answer.getAuthorId();
                if (authorId.equals(user.getId())) {
                        throw new ApiException(HttpStatus.BAD_REQUEST,
                "Author cannot vote on their own answer");
                }
                if (voteRepository.existsByTargetIdAndTargetTypeAndUserId(answerId, TargetType.ANSWER, user.getId())) {
                        throw new ApiException(HttpStatus.CONFLICT, "User has already voted on this answer");
                }
                answerRepository.incrementUpvoteCount(answerId);
                Vote newVote = Vote.builder()
                                .targetType(TargetType.ANSWER)
                                .targetId(answerId)
                                .user(user)
                                .voteType(VoteType.UPVOTE)
                                .createdAt(LocalDateTime.now())
                                .build();
                try {
                        voteRepository.save(newVote);
                } catch (DataIntegrityViolationException ex) {
                        throw new ApiException(HttpStatus.CONFLICT, "User has already voted on this answer");
                }
                return true;
        }

        public boolean hasMarkedGoodQuestion(UUID currentUserId, UUID postId){
                return voteRepository.existsByTargetIdAndTargetTypeAndUserId(postId, TargetType.QUESTION, currentUserId);
        }
}
