package com.gp.GP_backend.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only representation of a
 * {@link com.gp.GP_backend.domain.user.entity.CourseRegistered}
 * returned to the client.
 *
 * <p>
 * Mirrors every persisted field. {@code grade} will be {@code null} for
 * courses that are still in progress. {@code isCurrent} distinguishes active
 * enrollments from historical records.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRegistrationResponse {

    private UUID id;
    private UUID userId;
    private String courseCode;
    private String courseName;
    private Short semester;
    private Short academicYear;

    /** Null while the course is in progress; populated once graded. */
    private String grade;
    private BigDecimal result; // e.g. 85.5, 99.0, 43.6

    /**
     * {@code true} for active enrollments; {@code false} for historical records.
     */
    private Boolean isCurrent;
}