package com.gp.GP_backend.domain.user.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for {@code PATCH /api/v1/courses/{id}} — partially updates an
 * existing course registration.
 *
 * <p>
 * All fields are optional; only non-null values are applied (PATCH semantics).
 * Typical use cases: recording a final {@code grade} after the semester ends,
 * or setting {@code isCurrent} to {@code false} to archive the registration.
 */
@Data
public class UpdateCourseRegistrationRequest {

    /** Final grade (e.g. "A", "B+", "C"). Null while the course is in progress. */
    @Size(max = 5)
    private String grade;

    /** Final result (e.g. 85.5, 99.0, 43.6). Null while the course is in progress. */
    @DecimalMin(value = "0.0", inclusive = true, message = "Result must be at least 0.0")
    @DecimalMax(value = "100.0", inclusive = true, message = "Result must be at most 100.0")
    @Digits(integer = 3, fraction = 1, message = "Result must have up to 1 decimal place")
    private BigDecimal result;

    /**
     * Set to {@code false} to move a registration to historical records.
     * Once archived, it will not appear in the "current courses" endpoint.
     */
    private Boolean isCurrent;
}