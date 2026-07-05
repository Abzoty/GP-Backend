package com.gp.GP_backend.domain.course.service;

import com.gp.GP_backend.domain.course.dto.CourseRegistrationRequest;
import com.gp.GP_backend.domain.course.dto.CourseRegistrationResponse;
import com.gp.GP_backend.domain.course.dto.CourseRegistrationUpdateRequest;
import com.gp.GP_backend.domain.course.entity.CourseRegistration;
import com.gp.GP_backend.domain.course.repository.CourseRegistrationRepository;
import com.gp.GP_backend.domain.referencedata.service.ReferenceDataService;
import com.gp.GP_backend.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    @Mock
    private CourseRegistrationRepository courseRegistrationRepository;

    @Mock
    private CourseValidationService courseValidationService;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private CourseRegistrationService service;

    @Test
    void registerCourseShouldPersistDerivedGradesWhenBothScoresAreProvided() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        CourseRegistrationRequest request = CourseRegistrationRequest.builder()
                .code("CS101")
                .termWork(new BigDecimal("30"))
                .examWork(new BigDecimal("50"))
                .build();

        CourseRegistration saved = CourseRegistration.builder()
                .id(10L)
                .user(user)
                .code("CS101")
                .termWork(new BigDecimal("30"))
                .examWork(new BigDecimal("50"))
                .result(new BigDecimal("80"))
                .grade("A")
                .points(new BigDecimal("4.0"))
                .closed(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(courseRegistrationRepository.existsByUserAndCode(user, "CS101")).thenReturn(false);
        when(referenceDataService.resolveGrade(new BigDecimal("80"))).thenReturn("A");
        when(referenceDataService.resolvePoints(new BigDecimal("80"))).thenReturn(new BigDecimal("4.0"));
        when(courseRegistrationRepository.save(any(CourseRegistration.class))).thenReturn(saved);
        when(modelMapper.map(saved, CourseRegistrationResponse.class)).thenReturn(response(saved));

        CourseRegistrationResponse response = service.registerCourse(user, request);

        assertThat(response.getCode()).isEqualTo("CS101");
        assertThat(response.getResult()).isEqualByComparingTo("80");
        assertThat(response.getGrade()).isEqualTo("A");
        assertThat(response.getPoints()).isEqualByComparingTo("4.0");

        ArgumentCaptor<CourseRegistration> captor = ArgumentCaptor.forClass(CourseRegistration.class);
        verify(courseRegistrationRepository).save(captor.capture());
        assertThat(captor.getValue().getResult()).isEqualByComparingTo("80");
        assertThat(captor.getValue().getClosed()).isFalse();
    }

    @Test
    void registerCourseShouldRejectPartialGradeSubmission() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        CourseRegistrationRequest request = CourseRegistrationRequest.builder()
                .code("CS101")
                .termWork(new BigDecimal("30"))
                .build();

        when(courseRegistrationRepository.existsByUserAndCode(user, "CS101")).thenReturn(false);

        assertThrows(com.gp.GP_backend.shared.exception.ApiException.class,
                () -> service.registerCourse(user, request));

        verify(referenceDataService, never()).resolveGrade(any());
        verify(courseRegistrationRepository, never()).save(any());
    }

    @Test
    void updateRegistrationShouldRecalculateGradesAndClosedFlag() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        CourseRegistration existing = CourseRegistration.builder()
                .id(10L)
                .user(user)
                .code("CS101")
                .termWork(new BigDecimal("20"))
                .examWork(new BigDecimal("40"))
                .result(new BigDecimal("60"))
                .grade("C")
                .points(new BigDecimal("2.0"))
                .closed(false)
                .build();

        CourseRegistrationUpdateRequest request = CourseRegistrationUpdateRequest.builder()
                .termWork(new BigDecimal("35"))
                .examWork(new BigDecimal("55"))
                .closed(true)
                .build();

        CourseRegistration updated = CourseRegistration.builder()
                .id(10L)
                .user(user)
                .code("CS101")
                .termWork(new BigDecimal("35"))
                .examWork(new BigDecimal("55"))
                .result(new BigDecimal("90"))
                .grade("A")
                .points(new BigDecimal("4.0"))
                .closed(true)
                .build();

        when(courseRegistrationRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(referenceDataService.resolveGrade(new BigDecimal("90"))).thenReturn("A");
        when(referenceDataService.resolvePoints(new BigDecimal("90"))).thenReturn(new BigDecimal("4.0"));
        when(courseRegistrationRepository.save(existing)).thenReturn(updated);
        when(modelMapper.map(updated, CourseRegistrationResponse.class)).thenReturn(response(updated));

        CourseRegistrationResponse response = service.updateRegistration(user, 10L, request);

        assertThat(response.getResult()).isEqualByComparingTo("90");
        assertThat(response.getGrade()).isEqualTo("A");
        assertThat(response.getClosed()).isTrue();
    }

    private static User user(UUID id) {
        return User.builder()
                .id(id)
                .email("student@example.com")
                .passwordHash("hash")
                .fullName("Student Name")
                .build();
    }

    private static CourseRegistrationResponse response(CourseRegistration registration) {
        return CourseRegistrationResponse.builder()
                .id(registration.getId())
                .code(registration.getCode())
                .termWork(registration.getTermWork())
                .examWork(registration.getExamWork())
                .result(registration.getResult())
                .grade(registration.getGrade())
                .points(registration.getPoints())
                .closed(registration.getClosed())
                .createdAt(registration.getCreatedAt())
                .updatedAt(registration.getUpdatedAt())
                .build();
    }
}