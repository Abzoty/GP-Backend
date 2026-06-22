package com.gp.GP_backend.domain.course.service;

import com.gp.GP_backend.domain.course.dto.CourseRegistrationRequest;
import com.gp.GP_backend.domain.course.dto.CourseRegistrationResponse;
import com.gp.GP_backend.domain.course.dto.CourseRegistrationUpdateRequest;
import com.gp.GP_backend.domain.course.entity.CourseRegistration;
import com.gp.GP_backend.domain.course.repository.CourseRegistrationRepository;
import com.gp.GP_backend.domain.referencedata.service.ReferenceDataService;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for course registration.
 *
 * Responsibilities:
 * - Register a new course with derived grade/result/points
 * - Update an existing registration and recalculate grades
 * - Retrieve current (non-closed) and all registrations for a user
 * - Validate course existence and ownership
 *
 * All grade/result/points derivations happen here, not in the DTO or
 * controller.
 *
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourseRegistrationService {

    private final CourseRegistrationRepository courseRegistrationRepository;
    private final CourseValidationService courseValidationService;
    private final ReferenceDataService referenceDataService;
    private final ModelMapper modelMapper;

    /**
     * Registers a user for a course.
     *
     * - Validates course code exists in catalog
     * - Checks for duplicate (user, code) enrollment
     * - Computes result, grade, points from termWork and examWork
     * - Persists the registration
     *
     * @param user    the user registering
     * @param request the registration request
     * @return the created registration as a response DTO
     * @throws ApiException 400 if course code not found or invalid input
     * @throws ApiException 409 if duplicate registration already exists
     */
    @Transactional
    public CourseRegistrationResponse registerCourse(User user, CourseRegistrationRequest request) {
        // Validate course exists in catalog
        courseValidationService.validateCourseExists(request.getCode());

        // Check for existing registration (duplicate prevention)
        if (courseRegistrationRepository.existsByUserAndCode(user, request.getCode())) {
            log.warn("Duplicate registration attempt: user={}, course={}", user.getId(), request.getCode());
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "User is already registered for course: " + request.getCode());
        }

        // Compute derived fields
        BigDecimal result = request.getTermWork().add(request.getExamWork());
        String grade = referenceDataService.resolveGrade(result);
        BigDecimal points = referenceDataService.resolvePoints(result);

        // Create entity
        CourseRegistration registration = CourseRegistration.builder()
                .user(user)
                .code(request.getCode())
                .termWork(request.getTermWork())
                .examWork(request.getExamWork())
                .result(result)
                .grade(grade)
                .points(points)
                .closed(false)
                .build();

        try {
            CourseRegistration saved = courseRegistrationRepository.save(registration);
            log.info("Course registered: user={}, course={}, result={}, grade={}",
                    user.getId(), request.getCode(), result, grade);
            return mapToResponse(saved);
        } catch (DataIntegrityViolationException e) {
            // Race condition: another request registered the same course
            log.warn("Race condition detected for duplicate registration: user={}, course={}",
                    user.getId(), request.getCode());
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "User is already registered for this course (race condition detected)");
        }
    }

    /**
     * Updates an existing course registration.
     *
     * PATCH semantics: only non-null fields in the request are applied.
     * If termWork or examWork changes, result, grade, and points are recalculated.
     * The closed flag can be updated independently.
     *
     * @param user           the user who owns the registration
     * @param registrationId the ID of the registration to update
     * @param request        the update request
     * @return the updated registration as a response DTO
     * @throws ApiException 403 if user does not own the registration
     * @throws ApiException 404 if registration not found
     */
    @Transactional
    public CourseRegistrationResponse updateRegistration(User user, Long registrationId,
            CourseRegistrationUpdateRequest request) {
        CourseRegistration registration = getOwnedRegistration(user, registrationId);

        boolean needsRecalculation = false;

        // Update termWork if provided
        if (request.getTermWork() != null) {
            registration.setTermWork(request.getTermWork());
            needsRecalculation = true;
        }

        // Update examWork if provided
        if (request.getExamWork() != null) {
            registration.setExamWork(request.getExamWork());
            needsRecalculation = true;
        }

        // Update closed if provided
        if (request.getClosed() != null) {
            registration.setClosed(request.getClosed());
        }

        // Recalculate result, grade, points if either termWork or examWork changed
        if (needsRecalculation) {
            BigDecimal result = registration.getTermWork().add(registration.getExamWork());
            registration.setResult(result);
            registration.setGrade(referenceDataService.resolveGrade(result));
            registration.setPoints(referenceDataService.resolvePoints(result));

            log.info("Course registration updated: id={}, user={}, course={}, result={}, grade={}",
                    registrationId, user.getId(), registration.getCode(), result, registration.getGrade());
        }

        CourseRegistration saved = courseRegistrationRepository.save(registration);
        return mapToResponse(saved);
    }

    /**
     * Retrieves all current (non-closed) course registrations for a user.
     *
     * @param user the user
     * @return list of current registrations
     */
    @Transactional(readOnly = true)
    public List<CourseRegistrationResponse> getCurrentCourses(User user) {
        return courseRegistrationRepository.findCurrentByUser(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all course registrations for a user (current and closed).
     *
     * @param user the user
     * @return list of all registrations
     */
    @Transactional(readOnly = true)
    public List<CourseRegistrationResponse> getAllCourses(User user) {
        return courseRegistrationRepository.findByUser(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single registration by ID, verifying ownership.
     *
     * @param user           the user
     * @param registrationId the registration ID
     * @return the registration as a response DTO
     * @throws ApiException 403 if user does not own the registration
     * @throws ApiException 404 if registration not found
     */
    @Transactional(readOnly = true)
    public CourseRegistrationResponse getRegistration(User user, Long registrationId) {
        CourseRegistration registration = getOwnedRegistration(user, registrationId);
        return mapToResponse(registration);
    }

    /**
     * Deletes a course registration.
     *
     * @param user           the user who owns the registration
     * @param registrationId the registration ID
     * @throws ApiException 403 if user does not own the registration
     * @throws ApiException 404 if registration not found
     */
    @Transactional
    public void deleteRegistration(User user, Long registrationId) {
        CourseRegistration registration = getOwnedRegistration(user, registrationId);
        courseRegistrationRepository.delete(registration);
        log.info("Course registration deleted: id={}, user={}, course={}",
                registrationId, user.getId(), registration.getCode());
    }

    /**
     * Helper method: Retrieves a registration and verifies ownership.
     *
     * @param user           the user
     * @param registrationId the registration ID
     * @return the registration entity
     * @throws ApiException 403 if ownership does not match
     * @throws ApiException 404 if registration not found
     */
    private CourseRegistration getOwnedRegistration(User user, Long registrationId) {
        CourseRegistration registration = courseRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Registration not found"));

        if (!registration.getUser().getId().equals(user.getId())) {
            log.warn("Unauthorized access attempt: user={}, registration={}", user.getId(), registrationId);
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "You do not have permission to access this registration");
        }

        return registration;
    }

    /**
     * Maps a CourseRegistration entity to a response DTO.
     */
    private CourseRegistrationResponse mapToResponse(CourseRegistration registration) {
        return modelMapper.map(registration, CourseRegistrationResponse.class);
    }
}