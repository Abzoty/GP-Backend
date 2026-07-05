package com.gp.GP_backend.domain.onlinecourse.controller;

import com.gp.GP_backend.domain.onlinecourse.dto.OnlineCourseResponse;
import com.gp.GP_backend.domain.onlinecourse.service.OnlineCourseService;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnlineCourseControllerTest {

    @Mock
    private OnlineCourseService onlineCourseService;

    @InjectMocks
    private OnlineCourseController controller;

    @Test
    void getCoursesShouldReturnWrappedOnlineCourseList() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        OnlineCourseResponse response = OnlineCourseResponse.builder()
                .id("1")
                .courseCode("CS101")
                .title("Algorithms")
                .score(0.95)
                .build();

        when(onlineCourseService.getCoursesForStudent(userId)).thenReturn(List.of(response));

        ResponseEntity<ApiResponse<List<OnlineCourseResponse>>> entity = controller.getCourses(user);

        assertThat(entity.getStatusCode().value()).isEqualTo(200);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().isSuccess()).isTrue();
        assertThat(entity.getBody().getMessage()).isEqualTo("Courses retrieved successfully");
        assertThat(entity.getBody().getData()).hasSize(1);
        assertThat(entity.getBody().getData().get(0).getCourseCode()).isEqualTo("CS101");

        verify(onlineCourseService).getCoursesForStudent(userId);
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