package com.gp.GP_backend.domain.user.service;

import com.gp.GP_backend.domain.user.dto.CourseRegistrationResponse;
import com.gp.GP_backend.domain.user.dto.RegisterCourseRequest;
import com.gp.GP_backend.domain.user.dto.UpdateCourseRegistrationRequest;
import com.gp.GP_backend.domain.user.entity.CourseRegistered;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.repository.CourseRegisteredRepository;
import com.gp.GP_backend.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles course registration lifecycle: enrolling, updating, removing,
 * and querying a user's registered courses.
 */
@Service
@RequiredArgsConstructor
public class CourseRegistrationService {

    private final CourseRegisteredRepository courseRegisteredRepository;

    /**
     * Registers a new course for the authenticated user.
     *
     * <p>
     * A duplicate registration (same course code, academic year, and semester
     * for the same user) is rejected with {@code 409 CONFLICT}.
     *
     * @throws ApiException 409 if the course is already registered for that period.
     */
    @Transactional
    public CourseRegistrationResponse registerCourse(User user, RegisterCourseRequest request) {
        boolean alreadyRegistered = courseRegisteredRepository
                .existsByUserIdAndCourseCodeAndAcademicYearAndSemester(
                        user.getId(),
                        request.getCourseCode(),
                        request.getAcademicYear(),
                        request.getSemester());

        if (alreadyRegistered) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Course '" + request.getCourseCode() + "' is already registered for this period");
        }

        CourseRegistered registration = CourseRegistered.builder()
                .user(user)
                .courseCode(request.getCourseCode())
                .courseName(request.getCourseName())
                .semester(request.getSemester())
                .academicYear(request.getAcademicYear())
                .grade(null) // not graded yet
                .isCurrent(true) // active enrollment by default
                .build();

        return toResponse(courseRegisteredRepository.save(registration));
    }

    /**
     * Applies a partial update to an existing course registration.
     *
     * <p>
     * Ownership is verified — a user may only update their own registrations.
     *
     * @throws ApiException 404 if the registration does not exist.
     * @throws ApiException 403 if the registration belongs to a different user.
     */
    @Transactional
    public CourseRegistrationResponse updateRegistration(
            UUID registrationId, User user, UpdateCourseRegistrationRequest request) {

        CourseRegistered registration = getOwnedRegistration(registrationId, user.getId());

        Optional.ofNullable(request.getGrade()).ifPresent(registration::setGrade);
        Optional.ofNullable(request.getResult()).ifPresent(registration::setResult);
        Optional.ofNullable(request.getIsCurrent()).ifPresent(registration::setIsCurrent);

        return toResponse(courseRegisteredRepository.save(registration));
    }

    /**
     * Permanently removes a course registration from the database.
     *
     * <p>
     * Ownership is verified before deletion.
     *
     * @throws ApiException 404 if the registration does not exist.
     * @throws ApiException 403 if the registration belongs to a different user.
     */
    @Transactional
    public void deleteRegistration(UUID registrationId, User user) {
        CourseRegistered registration = getOwnedRegistration(registrationId, user.getId());
        courseRegisteredRepository.delete(registration);
    }

    /** Returns a specific course registration by its ID. */
    public CourseRegistrationResponse getCourseRegistration(UUID id, User currentUser) {
        CourseRegistered registration = getOwnedRegistration(id, currentUser.getId());
        return toResponse(registration);
    }

    /**
     * Returns only the active ({@code isCurrent = true}) course registrations
     * for the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<CourseRegistrationResponse> getCurrentCourses(UUID userId) {
        return courseRegisteredRepository.findByUserIdAndIsCurrentTrue(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns the full enrollment history (current and archived) for the
     * authenticated user.
     */
    @Transactional(readOnly = true)
    public List<CourseRegistrationResponse> getAllCourses(UUID userId) {
        return courseRegisteredRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Loads a registration by ID and verifies it belongs to the requesting user.
     *
     * @throws ApiException 404 if not found.
     * @throws ApiException 403 if owned by a different user.
     */
    private CourseRegistered getOwnedRegistration(UUID registrationId, UUID userId) {
        CourseRegistered registration = courseRegisteredRepository.findById(registrationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Course registration not found with id: " + registrationId));

        if (!registration.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "You do not have permission to modify this registration");
        }

        return registration;
    }

    /** Maps a {@link CourseRegistered} entity to its response DTO. */
    private CourseRegistrationResponse toResponse(CourseRegistered entity) {
        return CourseRegistrationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .courseCode(entity.getCourseCode())
                .courseName(entity.getCourseName())
                .semester(entity.getSemester())
                .academicYear(entity.getAcademicYear())
                .grade(entity.getGrade())
                .isCurrent(entity.getIsCurrent())
                .build();
    }
}