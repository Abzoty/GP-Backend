package com.gp.GP_backend.domain.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for course registrations.
 *
 * Contains all information about a course registration, including
 * the server-derived grade, result, and points (which may be null if
 * grades are pending).
 *
 * Nullable grade fields:
 * - result: null if termWork and examWork haven't been provided yet
 * - grade: null if termWork and examWork haven't been provided yet
 * - points: null if termWork and examWork haven't been provided yet
 * - termWork: null if not yet provided
 * - examWork: null if not yet provided
 *
 * Note: userId is NOT included in the response (to avoid redundancy;
 * the user context is from the JWT token).
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRegistrationResponse {

    /**
     * Database ID of the registration.
     */
    private Long id;

    /**
     * Course code (e.g., "CS301").
     */
    private String code;

    /**
     * Term work score (continuous assessment), or null if pending.
     */
    private BigDecimal termWork;

    /**
     * Exam work score (final exam), or null if pending.
     */
    private BigDecimal examWork;

    /**
     * Total numeric result (termWork + examWork), range 0–100, or null if pending.
     */
    private BigDecimal result;

    /**
     * Letter grade (A+, A, B, ..., F), or null if pending.
     */
    private String grade;

    /**
     * GPA points (0.0–4.0), or null if pending.
     */
    private BigDecimal points;

    /**
     * Whether the course is closed/graded.
     */
    private Boolean closed;

    /**
     * Timestamp when the registration was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp of last modification.
     */
    private LocalDateTime updatedAt;
}