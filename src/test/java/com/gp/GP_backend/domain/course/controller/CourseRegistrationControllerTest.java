package com.gp.GP_backend.domain.course.controller;

import com.gp.GP_backend.domain.course.dto.CourseRegistrationRequest;
import com.gp.GP_backend.domain.course.dto.CourseRegistrationResponse;
import com.gp.GP_backend.domain.course.dto.CourseRegistrationUpdateRequest;
import com.gp.GP_backend.domain.course.service.CourseRegistrationService;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationControllerTest {

    @Mock
    private CourseRegistrationService courseRegistrationService;

    @InjectMocks
    private CourseRegistrationController controller;

    @Test
    void registerCourseShouldReturnCreatedEnvelope() {
        User user = user(UUID.randomUUID());
        CourseRegistrationRequest request = CourseRegistrationRequest.builder().code("CS101").build();
        CourseRegistrationResponse response = CourseRegistrationResponse.builder().code("CS101").build();

        when(courseRegistrationService.registerCourse(user, request)).thenReturn(response);

        ResponseEntity<ApiResponse<CourseRegistrationResponse>> entity = controller.registerCourse(user, request);

        assertThat(entity.getStatusCode().value()).isEqualTo(201);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getData().getCode()).isEqualTo("CS101");
        verify(courseRegistrationService).registerCourse(user, request);
    }

    @Test
    void getAllCoursesShouldReturnAllRegistrations() {
        User user = user(UUID.randomUUID());
        CourseRegistrationResponse response = CourseRegistrationResponse.builder().code("CS101").build();
        when(courseRegistrationService.getAllCourses(user)).thenReturn(List.of(response));

        ResponseEntity<ApiResponse<List<CourseRegistrationResponse>>> entity = controller.getAllCourses(user);

        assertThat(entity.getStatusCode().value()).isEqualTo(200);
        assertThat(entity.getBody().getData()).hasSize(1);
    }

    @Test
    void getCurrentCoursesShouldReturnCurrentRegistrations() {
        User user = user(UUID.randomUUID());
        CourseRegistrationResponse response = CourseRegistrationResponse.builder().code("CS101").build();
        when(courseRegistrationService.getCurrentCourses(user)).thenReturn(List.of(response));

        ResponseEntity<ApiResponse<List<CourseRegistrationResponse>>> entity = controller.getCurrentCourses(user);

        assertThat(entity.getStatusCode().value()).isEqualTo(200);
        assertThat(entity.getBody().getData()).hasSize(1);
    }

    @Test
    void getRegistrationShouldReturnSingleRegistration() {
        User user = user(UUID.randomUUID());
        CourseRegistrationResponse response = CourseRegistrationResponse.builder().code("CS101").build();
        when(courseRegistrationService.getRegistration(user, 10L)).thenReturn(response);

        ResponseEntity<ApiResponse<CourseRegistrationResponse>> entity = controller.getRegistration(user, 10L);

        assertThat(entity.getStatusCode().value()).isEqualTo(200);
        assertThat(entity.getBody().getData().getCode()).isEqualTo("CS101");
    }

    @Test
    void updateRegistrationShouldReturnUpdatedRegistration() {
        User user = user(UUID.randomUUID());
        CourseRegistrationUpdateRequest request = CourseRegistrationUpdateRequest.builder()
                .termWork(new BigDecimal("30"))
                .examWork(new BigDecimal("50"))
                .build();
        CourseRegistrationResponse response = CourseRegistrationResponse.builder().code("CS101").build();
        when(courseRegistrationService.updateRegistration(user, 10L, request)).thenReturn(response);

        ResponseEntity<ApiResponse<CourseRegistrationResponse>> entity = controller.updateRegistration(user, 10L, request);

        assertThat(entity.getStatusCode().value()).isEqualTo(200);
        assertThat(entity.getBody().getData().getCode()).isEqualTo("CS101");
    }

    @Test
    void deleteRegistrationShouldReturnNoContent() {
        User user = user(UUID.randomUUID());

        ResponseEntity<Void> entity = controller.deleteRegistration(user, 10L);

        assertThat(entity.getStatusCode().value()).isEqualTo(204);
        verify(courseRegistrationService).deleteRegistration(user, 10L);
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