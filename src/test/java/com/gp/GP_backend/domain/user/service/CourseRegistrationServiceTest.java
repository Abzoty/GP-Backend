package com.gp.GP_backend.domain.user.service;

import com.gp.GP_backend.domain.user.dto.CourseRegistrationResponse;
import com.gp.GP_backend.domain.user.dto.RegisterCourseRequest;
import com.gp.GP_backend.domain.user.dto.UpdateCourseRegistrationRequest;
import com.gp.GP_backend.domain.user.entity.CourseRegistered;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.repository.CourseRegisteredRepository;
import com.gp.GP_backend.shared.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    @Mock
    private CourseRegisteredRepository courseRegisteredRepository;

    @InjectMocks
    private CourseRegistrationService courseRegistrationService;

    @Test
    void registerCourseShouldSaveWhenNotRegistered() {
        User user = user();
        RegisterCourseRequest request = registerRequest();

        when(courseRegisteredRepository.existsByUserIdAndCourseCodeAndAcademicYearAndSemester(
                user.getId(), request.getCourseCode(), request.getAcademicYear(), request.getSemester()))
                .thenReturn(false);

        CourseRegistered saved = registration(user);
        when(courseRegisteredRepository.save(any(CourseRegistered.class))).thenReturn(saved);

        CourseRegistrationResponse response = courseRegistrationService.registerCourse(user, request);

        assertEquals(saved.getCourseCode(), response.getCourseCode());
        assertEquals(saved.getAcademicYear(), response.getAcademicYear());
        assertTrue(response.getIsCurrent());
        verify(courseRegisteredRepository).save(any(CourseRegistered.class));
    }

    @Test
    void registerCourseShouldThrowConflictWhenAlreadyRegistered() {
        User user = user();
        RegisterCourseRequest request = registerRequest();

        when(courseRegisteredRepository.existsByUserIdAndCourseCodeAndAcademicYearAndSemester(
                user.getId(), request.getCourseCode(), request.getAcademicYear(), request.getSemester()))
                .thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> courseRegistrationService.registerCourse(user, request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(courseRegisteredRepository, never()).save(any(CourseRegistered.class));
    }

    @Test
    void registerCourseShouldTranslateDataIntegrityViolationToConflict() {
        User user = user();
        RegisterCourseRequest request = registerRequest();

        when(courseRegisteredRepository.existsByUserIdAndCourseCodeAndAcademicYearAndSemester(
                user.getId(), request.getCourseCode(), request.getAcademicYear(), request.getSemester()))
                .thenReturn(false);
        when(courseRegisteredRepository.save(any(CourseRegistered.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        ApiException ex = assertThrows(ApiException.class,
                () -> courseRegistrationService.registerCourse(user, request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void updateRegistrationShouldApplyPartialFieldsForOwner() {
        User user = user();
        UUID registrationId = UUID.randomUUID();
        CourseRegistered entity = registration(user);
        entity.setId(registrationId);

        UpdateCourseRegistrationRequest request = new UpdateCourseRegistrationRequest();
        request.setGrade("A");
        request.setResult(new BigDecimal("95.5"));
        request.setIsCurrent(false);

        when(courseRegisteredRepository.findById(registrationId)).thenReturn(Optional.of(entity));
        when(courseRegisteredRepository.save(entity)).thenReturn(entity);

        CourseRegistrationResponse response = courseRegistrationService.updateRegistration(registrationId, user, request);

        assertEquals("A", response.getGrade());
        assertEquals(new BigDecimal("95.5"), response.getResult());
        assertEquals(false, response.getIsCurrent());
        verify(courseRegisteredRepository).save(entity);
    }

    @Test
    void updateRegistrationShouldRejectNonOwner() {
        User owner = user();
        User attacker = User.builder().id(UUID.randomUUID()).build();
        UUID registrationId = UUID.randomUUID();

        CourseRegistered entity = registration(owner);
        entity.setId(registrationId);
        when(courseRegisteredRepository.findById(registrationId)).thenReturn(Optional.of(entity));

        ApiException ex = assertThrows(ApiException.class,
                () -> courseRegistrationService.updateRegistration(registrationId, attacker, new UpdateCourseRegistrationRequest()));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(courseRegisteredRepository, never()).save(any(CourseRegistered.class));
    }

    @Test
    void getCourseRegistrationShouldThrowNotFoundForMissingId() {
        User user = user();
        UUID missingId = UUID.randomUUID();

        when(courseRegisteredRepository.findById(missingId)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> courseRegistrationService.getCourseRegistration(missingId, user));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void getCurrentCoursesShouldReturnMappedList() {
        User user = user();
        CourseRegistered one = registration(user);
        CourseRegistered two = registration(user);
        two.setCourseCode("CS302");

        when(courseRegisteredRepository.findByUserIdAndIsCurrentTrue(user.getId())).thenReturn(List.of(one, two));

        List<CourseRegistrationResponse> current = courseRegistrationService.getCurrentCourses(user.getId());

        assertEquals(2, current.size());
        assertEquals("CS301", current.get(0).getCourseCode());
        assertEquals("CS302", current.get(1).getCourseCode());
    }

    private User user() {
        return User.builder()
                .id(UUID.randomUUID())
                .fullName("Test User")
                .email("test@gp.com")
                .build();
    }

    private RegisterCourseRequest registerRequest() {
        RegisterCourseRequest request = new RegisterCourseRequest();
        request.setCourseCode("CS301");
        request.setCourseName("Algorithms");
        request.setAcademicYear((short) 3);
        request.setSemester((short) 2);
        return request;
    }

    private CourseRegistered registration(User user) {
        return CourseRegistered.builder()
                .id(UUID.randomUUID())
                .user(user)
                .courseCode("CS301")
                .courseName("Algorithms")
                .academicYear((short) 3)
                .semester((short) 2)
                .grade(null)
                .result(null)
                .isCurrent(true)
                .build();
    }
}
