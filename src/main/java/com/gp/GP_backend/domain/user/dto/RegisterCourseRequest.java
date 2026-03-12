package com.gp.GP_backend.domain.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for {@code POST /api/v1/courses} — registers a course for the
 * authenticated user.
 *
 * <p>
 * The authenticated user's ID is resolved from the JWT token in the controller
 * and is never expected in this request body. {@code grade} defaults to null
 * (in-progress) and {@code isCurrent} defaults to {@code true} on creation.
 */
@Data
public class RegisterCourseRequest {

    @NotBlank(message = "Course code is required")
    @Size(max = 30)
    private String courseCode;

    @NotBlank(message = "Course name is required")
    @Size(max = 200)
    private String courseName;

    /** Semester within the academic year: 1 (first) or 2 (second). */
    @NotNull(message = "Semester is required")
    @Min(1)
    @Max(2)
    private Short semester;

    /** Academic year: 1–5. */
    @NotNull(message = "Academic year is required")
    @Min(1)
    @Max(5)
    private Short academicYear;
}