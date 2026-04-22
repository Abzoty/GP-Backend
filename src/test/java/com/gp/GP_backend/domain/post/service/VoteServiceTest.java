package com.gp.GP_backend.domain.post.service;

import com.gp.GP_backend.domain.post.entity.Answer;
import com.gp.GP_backend.domain.post.entity.TargetType;
import com.gp.GP_backend.domain.post.repository.AnswerRepository;
import com.gp.GP_backend.domain.post.repository.PostRepository;
import com.gp.GP_backend.domain.post.repository.VoteRepository;
import com.gp.GP_backend.domain.space.entity.Space;
import com.gp.GP_backend.domain.space.repository.SpaceMembershipRepository;
import com.gp.GP_backend.domain.space.repository.SpaceRepository;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private SpaceMembershipRepository spaceMembershipRepository;

    @InjectMocks
    private VoteService voteService;

    @Test
    void markGoodQuestionShouldSucceedForMemberOnActiveSpace() {
        User voter = user();
        UUID postId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        when(postRepository.findSpaceIdByPostId(postId)).thenReturn(spaceId);
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space(spaceId, true)));
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, voter.getId())).thenReturn(true);
        when(postRepository.findAuthorIdByPostId(postId)).thenReturn(authorId);
        when(voteRepository.existsByTargetIdAndTargetTypeAndUserId(postId, TargetType.QUESTION, voter.getId()))
            .thenReturn(false);

        boolean result = voteService.markGoodQuestion(postId, voter);

        assertTrue(result);
        verify(postRepository).incrementGoodQuestionCount(postId);
        verify(voteRepository).save(any());
    }

    @Test
    void markGoodQuestionShouldRejectDuplicateVote() {
        User voter = user();
        UUID postId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();

        when(postRepository.findSpaceIdByPostId(postId)).thenReturn(spaceId);
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space(spaceId, true)));
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, voter.getId())).thenReturn(true);
        when(postRepository.findAuthorIdByPostId(postId)).thenReturn(UUID.randomUUID());
        when(voteRepository.existsByTargetIdAndTargetTypeAndUserId(postId, TargetType.QUESTION, voter.getId()))
                .thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> voteService.markGoodQuestion(postId, voter));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(postRepository, never()).incrementGoodQuestionCount(postId);
    }

    @Test
    void markGoodQuestionShouldReturnConflictWhenSaveHitsUniqueConstraint() {
        User voter = user();
        UUID postId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();

        when(postRepository.findSpaceIdByPostId(postId)).thenReturn(spaceId);
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space(spaceId, true)));
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, voter.getId())).thenReturn(true);
        when(postRepository.findAuthorIdByPostId(postId)).thenReturn(UUID.randomUUID());
        when(voteRepository.existsByTargetIdAndTargetTypeAndUserId(postId, TargetType.QUESTION, voter.getId()))
                .thenReturn(false);
        when(voteRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        ApiException ex = assertThrows(ApiException.class, () -> voteService.markGoodQuestion(postId, voter));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void markGoodQuestionShouldRejectWhenUserNotMember() {
        User voter = user();
        UUID postId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();

        when(postRepository.findSpaceIdByPostId(postId)).thenReturn(spaceId);
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space(spaceId, true)));
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, voter.getId())).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> voteService.markGoodQuestion(postId, voter));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(voteRepository, never()).save(any());
    }

    @Test
    void markGoodQuestionShouldRejectSelfVote() {
        User voter = user();
        UUID postId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();

        when(postRepository.findSpaceIdByPostId(postId)).thenReturn(spaceId);
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space(spaceId, true)));
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, voter.getId())).thenReturn(true);
        when(postRepository.findAuthorIdByPostId(postId)).thenReturn(voter.getId());

        ApiException ex = assertThrows(ApiException.class, () -> voteService.markGoodQuestion(postId, voter));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(postRepository, never()).incrementGoodQuestionCount(postId);
    }

    @Test
    void markGoodQuestionShouldRejectInactiveSpace() {
        User voter = user();
        UUID postId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();

        when(postRepository.findSpaceIdByPostId(postId)).thenReturn(spaceId);
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space(spaceId, false)));

        ApiException ex = assertThrows(ApiException.class, () -> voteService.markGoodQuestion(postId, voter));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(voteRepository, never()).save(any());
    }

    @Test
    void upvoteGoodAnswerShouldSucceedWhenValidMemberAndNotOwner() {
        User voter = user();
        UUID answerId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Answer answer = Answer.builder().id(answerId).postId(postId).authorId(authorId).build();

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(postRepository.findSpaceIdByPostId(postId)).thenReturn(spaceId);
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space(spaceId, true)));
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, voter.getId())).thenReturn(true);
        when(voteRepository.existsByTargetIdAndTargetTypeAndUserId(answerId, TargetType.ANSWER, voter.getId()))
                .thenReturn(false);

        boolean result = voteService.upvoteGoodAnswer(answerId, voter);

        assertTrue(result);
        verify(answerRepository).incrementUpvoteCount(answerId);
        verify(voteRepository).save(any());
    }

    @Test
    void upvoteGoodAnswerShouldRejectDuplicateVote() {
        User voter = user();
        UUID answerId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();

        Answer answer = Answer.builder().id(answerId).postId(postId).authorId(UUID.randomUUID()).build();

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(postRepository.findSpaceIdByPostId(postId)).thenReturn(spaceId);
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space(spaceId, true)));
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, voter.getId())).thenReturn(true);
        when(voteRepository.existsByTargetIdAndTargetTypeAndUserId(answerId, TargetType.ANSWER, voter.getId()))
                .thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> voteService.upvoteGoodAnswer(answerId, voter));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(answerRepository, never()).incrementUpvoteCount(answerId);
    }

    @Test
    void upvoteGoodAnswerShouldReturnConflictWhenSaveHitsUniqueConstraint() {
        User voter = user();
        UUID answerId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();

        Answer answer = Answer.builder().id(answerId).postId(postId).authorId(UUID.randomUUID()).build();

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(postRepository.findSpaceIdByPostId(postId)).thenReturn(spaceId);
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space(spaceId, true)));
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, voter.getId())).thenReturn(true);
        when(voteRepository.existsByTargetIdAndTargetTypeAndUserId(answerId, TargetType.ANSWER, voter.getId()))
                .thenReturn(false);
        when(voteRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        ApiException ex = assertThrows(ApiException.class, () -> voteService.upvoteGoodAnswer(answerId, voter));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void upvoteGoodAnswerShouldRejectInactiveSpace() {
        User voter = user();
        UUID answerId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();

        Answer answer = Answer.builder().id(answerId).postId(postId).authorId(UUID.randomUUID()).build();

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(postRepository.findSpaceIdByPostId(postId)).thenReturn(spaceId);
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space(spaceId, false)));

        ApiException ex = assertThrows(ApiException.class, () -> voteService.upvoteGoodAnswer(answerId, voter));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(voteRepository, never()).save(any());
    }

    @Test
    void upvoteGoodAnswerShouldRejectSelfVote() {
        User voter = user();
        UUID answerId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();

        Answer answer = Answer.builder().id(answerId).postId(postId).authorId(voter.getId()).build();

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(postRepository.findSpaceIdByPostId(postId)).thenReturn(spaceId);
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space(spaceId, true)));
        when(spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, voter.getId())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> voteService.upvoteGoodAnswer(answerId, voter));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(voteRepository, never()).save(any());
    }

    @Test
    void upvoteGoodAnswerShouldThrowNotFoundWhenAnswerMissing() {
        User voter = user();
        UUID answerId = UUID.randomUUID();

        when(answerRepository.findById(answerId)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> voteService.upvoteGoodAnswer(answerId, voter));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void hasMarkedGoodQuestionShouldReturnRepositoryResult() {
        UUID postId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        when(voteRepository.existsByTargetIdAndTargetTypeAndUserId(postId, TargetType.QUESTION, currentUserId))
                .thenReturn(false);

        boolean result = voteService.hasMarkedGoodQuestion(currentUserId, postId);

        assertFalse(result);
    }

    private User user() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("voter@gp.com")
                .fullName("Voter")
                .build();
    }

    private Space space(UUID id, boolean isActive) {
        return Space.builder()
                .id(id)
                .name("Algorithms")
                .slug("algorithms")
                .isActive(isActive)
                .build();
    }
}
