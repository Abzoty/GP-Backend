package com.gp.GP_backend.domain.user.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Public-safe projection of a
 * {@link com.gp.GP_backend.domain.user.entity.User}.
 *
 * <p>
 * Excludes sensitive fields ({@code passwordHash}, {@code isActive},
 * timestamps)
 * that the client has no business seeing.
 *
 * <p>
 * Populated by ModelMapper — field names must match the entity's getter names
 * exactly
 * (or be configured with an explicit mapping in
 * {@link com.gp.GP_backend.config.ModelMapperConfig}).
 */
@Data
@NoArgsConstructor
public class UserResponse {

    /** UUID string of the user. */
    private String id;

    private String email;

    private String fullName;

    /** University student ID (nullable). */
    private String studentId;

    /** Academic year (1–5). */
    private Short academicYear;

    /** Current semester number (1–10). */
    private Short currentSemester;

    /** GPA as DECIMAL(4,2). */
    private BigDecimal gpa;

    /** Department/major name. */
    private String department;

    /** URL of the user's profile picture. */
    private String imageUrl;

    /** Short bio. */
    private String bio;
}