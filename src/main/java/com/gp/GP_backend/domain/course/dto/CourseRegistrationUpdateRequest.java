package com.gp.GP_backend.domain.course.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for updating an existing course registration (PATCH semantics).
 *
 * Client can update any of:
 * - termWork: continuous assessment score (0–40, optional)
 * - examWork: final exam score (0–60, optional)
 * - closed: whether the course is closed (optional)
 *
 * If termWork or examWork changes, result, grade, and points are recalculated.
 * If only closed changes, grade/result/points remain untouched.
 *
 * All fields are optional (null = no change for that field).
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRegistrationUpdateRequest {

    /**
     * Term work score (continuous assessment), optional.
     * If provided, must be between 0 and 40 (inclusive).
     */
    @DecimalMin(value = "0", inclusive = true, message = "Term work must be at least 0")
    @DecimalMax(value = "40", inclusive = true, message = "Term work must not exceed 40")
    private BigDecimal termWork;

    /**
     * Exam work score (final exam), optional.
     * If provided, must be between 0 and 60 (inclusive).
     */
    @DecimalMin(value = "0", inclusive = true, message = "Exam work must be at least 0")
    @DecimalMax(value = "60", inclusive = true, message = "Exam work must not exceed 60")
    private BigDecimal examWork;

    /**
     * Whether the course is closed, optional.
     * If not provided, the value is not changed.
     */
    private Boolean closed;
}