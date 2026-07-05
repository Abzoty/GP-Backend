package com.gp.GP_backend.domain.post.controller;

import com.gp.GP_backend.domain.post.dto.EditAnswerRequest;
import com.gp.GP_backend.domain.post.dto.EditPostRequest;
import com.gp.GP_backend.domain.post.service.PostService;
import com.gp.GP_backend.domain.post.service.VoteService;
import com.gp.GP_backend.domain.space.service.SpaceService;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @Mock
    private PostService postService;

    @Mock
    private SpaceService spaceService;

    @Mock
    private VoteService voteService;

    @InjectMocks
    private PostController controller;

    @Test
    void editPostShouldReturnUpdatedFlag() {
        UUID postId = UUID.randomUUID();
        User user = user(UUID.randomUUID());
        EditPostRequest request = new EditPostRequest();
        request.setTitle("Updated title");
        request.setBody("Updated body");

        when(postService.editPost(request, user.getId(), postId)).thenReturn(true);

        ResponseEntity<ApiResponse<Boolean>> entity = controller.editPost(postId, request, user);

        assertThat(entity.getStatusCode().value()).isEqualTo(200);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getData()).isTrue();

        verify(postService).editPost(request, user.getId(), postId);
    }

    @Test
    void deletePostShouldReturnDeletedFlag() {
        UUID postId = UUID.randomUUID();
        User user = user(UUID.randomUUID());

        when(postService.deletePost(postId, user.getId())).thenReturn(true);

        ResponseEntity<ApiResponse<Boolean>> entity = controller.deletePost(postId, user);

        assertThat(entity.getStatusCode().value()).isEqualTo(200);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getData()).isTrue();

        verify(postService).deletePost(postId, user.getId());
    }

    @Test
    void editAnswerShouldReturnUpdatedFlag() {
        UUID answerId = UUID.randomUUID();
        User user = user(UUID.randomUUID());
        EditAnswerRequest request = new EditAnswerRequest();
        request.setBody("Updated answer body");

        when(postService.editAnswer(request, user.getId(), answerId)).thenReturn(true);

        ResponseEntity<ApiResponse<Boolean>> entity = controller.editAnswer(answerId, request, user);

        assertThat(entity.getStatusCode().value()).isEqualTo(200);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getData()).isTrue();

        verify(postService).editAnswer(request, user.getId(), answerId);
    }

    @Test
    void deleteAnswerShouldReturnDeletedFlag() {
        UUID answerId = UUID.randomUUID();
        User user = user(UUID.randomUUID());

        when(postService.deleteAnswer(answerId, user.getId())).thenReturn(true);

        ResponseEntity<ApiResponse<Boolean>> entity = controller.deleteAnswer(answerId, user);

        assertThat(entity.getStatusCode().value()).isEqualTo(200);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getData()).isTrue();

        verify(postService).deleteAnswer(answerId, user.getId());
    }

    private static User user(UUID id) {
        return User.builder()
                .id(id)
                .email("student@example.com")
                .passwordHash("hash")
                .fullName("Student Name")
                .build();
    }
}