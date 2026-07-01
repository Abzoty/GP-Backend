package com.gp.GP_backend.domain.onlinecourse.service;

import com.gp.GP_backend.domain.course.entity.CourseRegistration;
import com.gp.GP_backend.domain.course.repository.CourseRegistrationRepository;
import com.gp.GP_backend.domain.onlinecourse.dto.OnlineCourseResponse;
import com.gp.GP_backend.domain.onlinecourse.entity.OnlineCourse;
import com.gp.GP_backend.domain.onlinecourse.repository.OnlineCourseRepository;
import com.gp.GP_backend.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnlineCourseServiceTest {

    @Mock
    private OnlineCourseRepository onlineCourseRepository;

    @Mock
    private CourseRegistrationRepository courseRegistrationRepository;

    @InjectMocks
    private OnlineCourseService service;

    @Test
    void getCoursesForStudentShouldReturnEmptyListWhenNoActiveRegistrationsExist() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);

        when(courseRegistrationRepository.findByUserIdAndClosedFalse(userId)).thenReturn(List.of());

        List<OnlineCourseResponse> responses = service.getCoursesForStudent(userId);

        assertThat(responses).isEmpty();
        verify(onlineCourseRepository, never()).findByCourseCodeInOrderByScoreDesc(anyList());
    }

    @Test
    void getCoursesForStudentShouldFilterDuplicateAndBlankCodesAndMapResponses() {
        UUID userId = UUID.randomUUID();

        when(courseRegistrationRepository.findByUserIdAndClosedFalse(userId)).thenReturn(List.of(
                registration("CS101", false),
                registration("CS101", false),
                registration("", false),
                registration(null, false),
                registration("IT202", false),
                registration("MATH303", true)));

        OnlineCourse first = onlineCourse("1", "CS101", "Algorithms", 4.8, 1.99, 0.92);
        OnlineCourse second = onlineCourse("2", "IT202", "Networks", 4.5, 0.0, 0.81);
        when(onlineCourseRepository.findByCourseCodeInOrderByScoreDesc(List.of("CS101", "IT202")))
                .thenReturn(List.of(first, second));

        List<OnlineCourseResponse> responses = service.getCoursesForStudent(userId);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo("1");
        assertThat(responses.get(0).getCourseCode()).isEqualTo("CS101");
        assertThat(responses.get(0).getTitle()).isEqualTo("Algorithms");
        assertThat(responses.get(0).getScore()).isEqualTo(0.92);
        assertThat(responses.get(1).getId()).isEqualTo("2");
        assertThat(responses.get(1).getCourseCode()).isEqualTo("IT202");
        assertThat(responses.get(1).getTitle()).isEqualTo("Networks");
        assertThat(responses.get(1).getScore()).isEqualTo(0.81);
    }

    private static User user(UUID id) {
        return User.builder()
                .id(id)
                .email("student@example.com")
                .passwordHash("hash")
                .fullName("Student Name")
                .build();
    }

    private static CourseRegistration registration(String code, boolean closed) {
        return CourseRegistration.builder()
                .code(code)
                .closed(closed)
                .build();
    }

    private static OnlineCourse onlineCourse(String id, String code, String title, double rating, double price, double score) {
        return OnlineCourse.builder()
                .id(id)
                .courseCode(code)
                .courseName(title + " Course")
                .source("coursera")
                .title(title)
                .url("https://example.com/" + id)
                .description(title + " description")
                .rating(rating)
                .reviews(120)
                .price(price)
                .score(score)
                .lastUpdated(LocalDateTime.of(2026, 1, 1, 12, 0))
                .build();
    }
}