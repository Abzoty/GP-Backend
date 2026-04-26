package com.gp.GP_backend.domain.post.service;

import com.gp.GP_backend.domain.post.entity.Answer;
import com.gp.GP_backend.domain.post.entity.Post;
import com.gp.GP_backend.domain.post.repository.AnswerRepository;
import com.gp.GP_backend.domain.post.repository.PostRepository;
import com.gp.GP_backend.domain.post.repository.VoteRepository;
import com.gp.GP_backend.domain.space.repository.SpaceMembershipRepository;
import com.gp.GP_backend.domain.space.repository.SpaceRepository;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.repository.UserRepository;
import com.gp.GP_backend.domain.user.service.GamificationService;
import com.gp.GP_backend.domain.user.service.UserService;
import com.gp.GP_backend.shared.exception.ApiException;
import com.gp.GP_backend.shared.util.XpCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceGamificationTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private AnswerRepository answerRepository;
    @Mock
    private SpaceMembershipRepository spaceMembershipRepository;
    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SpaceRepository spaceRepository;
    @Mock
    private VoteRepository voteRepository;
    @Mock
    private GamificationService gamificationService;

    @InjectMocks
    private PostService postService;

    @Test
    void markQuestionAsSolvedShouldRejectSelfAcceptedAnswer() {
        UUID postId = UUID.randomUUID();
        UUID answerId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        User actor = User.builder().id(authorId).build();

        Post post = Post.builder().id(postId).authorId(authorId).build();
        Answer answer = Answer.builder().id(answerId).postId(postId).authorId(authorId).build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));

        ApiException ex = assertThrows(ApiException.class,
                () -> postService.markQuestionAsSolved(postId, answerId, actor));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(answerRepository, never()).markAsAccepted(answerId);
        verify(postRepository, never()).markAsSolved(postId, answerId);
        verify(gamificationService, never()).awardXp(authorId,
                XpCalculator.EVENT_ANSWER_ACCEPTED,
                XpCalculator.XP_ANSWER_ACCEPTED,
                answerId,
                XpCalculator.REF_ANSWER);
    }

    @Test
    void markQuestionAsSolvedShouldAwardXpForDifferentAnswerAuthor() {
        UUID postId = UUID.randomUUID();
        UUID answerId = UUID.randomUUID();
        UUID questionAuthorId = UUID.randomUUID();
        UUID answerAuthorId = UUID.randomUUID();
        User actor = User.builder().id(questionAuthorId).build();

        Post post = Post.builder().id(postId).authorId(questionAuthorId).build();
        Answer answer = Answer.builder().id(answerId).postId(postId).authorId(answerAuthorId).build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));

        boolean result = postService.markQuestionAsSolved(postId, answerId, actor);

        assertEquals(true, result);
        verify(answerRepository).markAsAccepted(answerId);
        verify(postRepository).markAsSolved(postId, answerId);
        verify(gamificationService).awardXp(answerAuthorId,
                XpCalculator.EVENT_ANSWER_ACCEPTED,
                XpCalculator.XP_ANSWER_ACCEPTED,
                answerId,
                XpCalculator.REF_ANSWER);
    }
}
