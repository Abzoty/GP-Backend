package com.gp.GP_backend.domain.post.service;

import com.gp.GP_backend.domain.post.repository.VoteRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.gp.GP_backend.domain.post.entity.Vote;
import com.gp.GP_backend.domain.post.entity.VoteType;
import com.gp.GP_backend.domain.notification.service.NotificationService;
import com.gp.GP_backend.domain.post.entity.TargetType;
import com.gp.GP_backend.domain.post.entity.Answer;
import com.gp.GP_backend.domain.post.entity.Post;
import com.gp.GP_backend.domain.post.repository.AnswerRepository;
import com.gp.GP_backend.domain.post.repository.PostRepository;
import com.gp.GP_backend.domain.space.entity.Space;
import com.gp.GP_backend.domain.space.repository.SpaceMembershipRepository;
import com.gp.GP_backend.domain.space.repository.SpaceRepository;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.service.GamificationService;
import com.gp.GP_backend.shared.exception.ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import com.gp.GP_backend.shared.util.XpCalculator;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VoteService {
        private final VoteRepository voteRepository;
        private final PostRepository postRepository;
        private final AnswerRepository answerRepository;
        private final SpaceRepository spaceRepository;
        private final SpaceMembershipRepository spaceMembershipRepository;
        private final GamificationService gamificationService;
        private final NotificationService notificationService;

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
                        Vote savedVote = voteRepository.save(newVote);
                        notificationService.notifyGoodQuestionMarked(postId, user);
                        // Award the post's author, not the voter.
                        gamificationService.awardXp(
                                        authorId,
                                        XpCalculator.EVENT_GOOD_QUESTION,
                                        XpCalculator.XP_GOOD_QUESTION,
                                        savedVote.getId(),
                                        XpCalculator.REF_VOTE);
                } catch (DataIntegrityViolationException ex) {
                        throw new ApiException(HttpStatus.CONFLICT, "User has already voted on this question");
                }
                return true;
        }

        @Transactional
        public boolean upvoteGoodAnswer(UUID answerId, User user) {
                Answer answer = answerRepository.findById(answerId)
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Answer not found"));
                UUID postId = answer.getPost().getId();
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
                UUID authorId = answer.getAuthor().getId();
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
                        Vote savedVote = voteRepository.save(newVote);
                        notificationService.notifyUpvoteAnswerReceived(answerId, user);
                        // Award the answer's author, not the voter.
                        gamificationService.awardXp(
                                        authorId,
                                        XpCalculator.EVENT_ANSWER_UPVOTED,
                                        XpCalculator.XP_ANSWER_UPVOTED,
                                        savedVote.getId(),
                                        XpCalculator.REF_VOTE);
                } catch (DataIntegrityViolationException ex) {
                        throw new ApiException(HttpStatus.CONFLICT, "User has already voted on this answer");
                }

                return true;
        }

        @Transactional
        public boolean removeGoodQuestion(UUID postId, User user) {
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

                Vote vote = voteRepository
                                .findByTargetIdAndTargetTypeAndUserId(postId, TargetType.QUESTION, user.getId())
                                .orElse(null);
                if (vote == null) {
                        return true;
                }

                postRepository.decrementGoodQuestionCount(postId);

                UUID authorId = postRepository.findAuthorIdByPostId(postId);
                gamificationService.revokeXp(
                                authorId,
                                XpCalculator.EVENT_GOOD_QUESTION,
                                vote.getId(),
                                XpCalculator.REF_VOTE);

                voteRepository.delete(vote);
                return true;
        }

        @Transactional
        public boolean removeUpvote(UUID answerId, User user) {
                Answer answer = answerRepository.findById(answerId)
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Answer not found"));
                UUID postId = answer.getPost().getId();
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

                Vote vote = voteRepository
                                .findByTargetIdAndTargetTypeAndUserId(answerId, TargetType.ANSWER, user.getId())
                                .orElse(null);
                if (vote == null) {
                        return true;
                }

                answerRepository.decrementUpvoteCount(answerId);

                UUID authorId = answer.getAuthor().getId();
                gamificationService.revokeXp(
                                authorId,
                                XpCalculator.EVENT_ANSWER_UPVOTED,
                                vote.getId(),
                                XpCalculator.REF_VOTE);

                voteRepository.delete(vote);
                return true;
        }

        public boolean hasMarkedGoodQuestion(UUID currentUserId, UUID postId) {
                return voteRepository.existsByTargetIdAndTargetTypeAndUserId(postId, TargetType.QUESTION,
                                currentUserId);
        }
}