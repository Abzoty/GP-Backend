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
 * Grade update rules (both-or-nothing):
 * - If termWork is provided: examWork must also be provided (cannot update only
 * one)
 * - If examWork is provided: termWork must also be provided (cannot update only
 * one)
 * - If both termWork and examWork are provided: result, grade, and points are
 * recalculated
 * - If both are null (and not part of the request): existing values are
 * preserved
 * - If both need to be cleared (set to null): provide both explicitly as null
 *
 * Independent updates:
 * - If only closed changes: grade/result/points remain untouched
 * - closed can be updated independently without touching grades
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
     * If provided, must be between 0 and 40 (inclusive), or null.
     * If examWork is provided, this must also be provided (or null together).
     */
    @DecimalMin(value = "0", inclusive = true, message = "Term work must be at least 0")
    @DecimalMax(value = "40", inclusive = true, message = "Term work must not exceed 40")
    private BigDecimal termWork;

    /**
     * Exam work score (final exam), optional.
     * If provided, must be between 0 and 60 (inclusive), or null.
     * If termWork is provided, this must also be provided (or null together).
     */
    @DecimalMin(value = "0", inclusive = true, message = "Exam work must be at least 0")
    @DecimalMax(value = "60", inclusive = true, message = "Exam work must not exceed 60")
    private BigDecimal examWork;

    /**
     * Whether the course is closed, optional.
     * If not provided, the value is not changed.
     * Can be updated independently of grade fields.
     */
    private Boolean closed;
}