package com.gp.GP_backend.domain.course.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for registering a new course.
 *
 * Client supplies:
 * - code: the course code (validated against catalog)
 * - termWork: continuous assessment score (0–40)
 * - examWork: final exam score (0–60)
 *
 * Server derives:
 * - result = termWork + examWork
 * - grade (from grade mapping)
 * - points (from grade mapping)
 * - closed = false (default)
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRegistrationRequest {

    /**
     * Course code from catalog (e.g., "CS301").
     * Validated against ReferenceDataService at service layer.
     */
    @NotBlank(message = "Course code is required")
    @Size(max = 20, message = "Course code must not exceed 20 characters")
    private String code;

    /**
     * Term work score (continuous assessment).
     * Must be between 0 and 40 (inclusive).
     */
    @NotNull(message = "Term work score is required")
    @DecimalMin(value = "0", inclusive = true, message = "Term work must be at least 0")
    @DecimalMax(value = "40", inclusive = true, message = "Term work must not exceed 40")
    private BigDecimal termWork;

    /**
     * Exam work score (final exam).
     * Must be between 0 and 60 (inclusive).
     */
    @NotNull(message = "Exam work score is required")
    @DecimalMin(value = "0", inclusive = true, message = "Exam work must be at least 0")
    @DecimalMax(value = "60", inclusive = true, message = "Exam work must not exceed 60")
    private BigDecimal examWork;
}