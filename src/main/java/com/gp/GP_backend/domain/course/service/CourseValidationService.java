package com.gp.GP_backend.domain.course.service;

import com.gp.GP_backend.domain.referencedata.service.ReferenceDataService;
import com.gp.GP_backend.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Validates course-related business rules.
 *
 * Responsible for:
 * - Verifying course codes exist in the catalog
 * - Other course-specific validations as needed
 *
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourseValidationService {

    private final ReferenceDataService referenceDataService;

    /**
     * Validates that a course code exists in the catalog.
     *
     * @param code the course code to validate
     * @throws ApiException with 400 Bad Request if code is not found
     */
    public void validateCourseExists(String code) {
        if (!referenceDataService.findCourse(code).isPresent()) {
            log.warn("Attempt to register non-existent course code: {}", code);
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Course code '" + code + "' does not exist in the catalog");
        }
    }
}