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


@Service
@RequiredArgsConstructor
@Slf4j
public class CourseRegistrationService {

    private final CourseRegistrationRepository courseRegistrationRepository;
    private final CourseValidationService courseValidationService;
    private final ReferenceDataService referenceDataService;
    private final ModelMapper modelMapper;


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

        // Validate and compute derived fields
        BigDecimal result = null;
        String grade = null;
        BigDecimal points = null;

        boolean hasTermWork = request.getTermWork() != null;
        boolean hasExamWork = request.getExamWork() != null;

        // Both-or-nothing validation: either both provided or both null
        if (hasTermWork != hasExamWork) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Both termWork and examWork must be provided together, or both left null. " +
                            "Partial grade submission is not allowed.");
        }

        // If both grades are provided, calculate derived fields
        if (hasTermWork && hasExamWork) {
            result = request.getTermWork().add(request.getExamWork());
            grade = referenceDataService.resolveGrade(result);
            points = referenceDataService.resolvePoints(result);

            log.debug("Grades provided during registration: result={}, grade={}, points={}", result, grade, points);
        } else {
            log.debug("Course registered with pending grades (null state)");
        }

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
            if (grade != null) {
                log.info("Course registered with grades: user={}, course={}, result={}, grade={}",
                        user.getId(), request.getCode(), result, grade);
            } else {
                log.info("Course registered with pending grades: user={}, course={}",
                        user.getId(), request.getCode());
            }
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

    @Transactional
    public CourseRegistrationResponse updateRegistration(User user, Long registrationId,
            CourseRegistrationUpdateRequest request) {
        CourseRegistration registration = getOwnedRegistration(user, registrationId);

        boolean needsRecalculation = false;
        BigDecimal newTermWork = registration.getTermWork();
        BigDecimal newExamWork = registration.getExamWork();

        // Update termWork if provided
        if (request.getTermWork() != null) {
            newTermWork = request.getTermWork();
            needsRecalculation = true;
        }

        // Update examWork if provided
        if (request.getExamWork() != null) {
            newExamWork = request.getExamWork();
            needsRecalculation = true;
        }

        // Validate grade state consistency
        if (needsRecalculation) {
            boolean hasTermWork = newTermWork != null;
            boolean hasExamWork = newExamWork != null;

            // Both-or-nothing validation for grades
            if (hasTermWork != hasExamWork) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Both termWork and examWork must be provided together, or both left null. " +
                                "Partial grade submission is not allowed.");
            }

            // Recalculate derived fields if both grades are present
            if (hasTermWork && hasExamWork) {
                BigDecimal result = newTermWork.add(newExamWork);
                registration.setTermWork(newTermWork);
                registration.setExamWork(newExamWork);
                registration.setResult(result);
                registration.setGrade(referenceDataService.resolveGrade(result));
                registration.setPoints(referenceDataService.resolvePoints(result));

                log.info("Course registration updated with grades: id={}, user={}, course={}, " +
                        "result={}, grade={}, points={}",
                        registrationId, user.getId(), registration.getCode(), result,
                        registration.getGrade(), registration.getPoints());
            } else {
                // Both grades being cleared to null (pending state)
                registration.setTermWork(null);
                registration.setExamWork(null);
                registration.setResult(null);
                registration.setGrade(null);
                registration.setPoints(null);

                log.info("Course registration updated to pending grades: id={}, user={}, course={}",
                        registrationId, user.getId(), registration.getCode());
            }
        }

        // Update closed if provided (independent of grades)
        if (request.getClosed() != null) {
            registration.setClosed(request.getClosed());
            log.debug("Course registration closed flag updated: id={}, closed={}",
                    registrationId, request.getClosed());
        }

        CourseRegistration saved = courseRegistrationRepository.save(registration);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CourseRegistrationResponse> getCurrentCourses(User user) {
        return courseRegistrationRepository.findCurrentByUser(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public List<CourseRegistrationResponse> getAllCourses(User user) {
        return courseRegistrationRepository.findByUser(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public CourseRegistrationResponse getRegistration(User user, Long registrationId) {
        CourseRegistration registration = getOwnedRegistration(user, registrationId);
        return mapToResponse(registration);
    }


    @Transactional
    public void deleteRegistration(User user, Long registrationId) {
        CourseRegistration registration = getOwnedRegistration(user, registrationId);
        courseRegistrationRepository.delete(registration);
        log.info("Course registration deleted: id={}, user={}, course={}",
                registrationId, user.getId(), registration.getCode());
    }


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


    private CourseRegistrationResponse mapToResponse(CourseRegistration registration) {
        return modelMapper.map(registration, CourseRegistrationResponse.class);
    }
}