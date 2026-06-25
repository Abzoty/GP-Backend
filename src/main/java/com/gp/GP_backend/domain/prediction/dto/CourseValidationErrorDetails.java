package com.gp.GP_backend.domain.prediction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Data payload returned to the frontend inside the HTTP 422 response body
 * when the ML model cannot run because the student's course data is incomplete.
 *
 * <p>Example JSON (inside the {@code ApiResponse.data} envelope):
 * <pre>{@code
 * {
 * "error": "INSUFFICIENT_COURSE_DATA",
 * "missingCourses": ["CS401", "MATH301"],
 * "incompleteCourses": [{"code": "PHYS201", "missing_fields": ["grade"]}]
 * }
 * }</pre>
 *
 * The frontend should:
 * <ul>
 * <li>Display {@code missingCourses} as "courses you haven't registered
 * for".</li>
 * <li>Display {@code incompleteCourses} as "courses still awaiting a final
 * grade".</li>
 * <li>Direct the student to the Course Registration module to resolve either
 * list.</li>
 * </ul>
 *
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseValidationErrorDetails {

    /**
     * Stable machine-readable error code — always
     * {@code "INSUFFICIENT_COURSE_DATA"}.
     */
    @Builder.Default
    private String error = "INSUFFICIENT_COURSE_DATA";

    /**
     * Course codes that the model requires but that are entirely absent
     * from the student's registrations.
     */
    private List<String> missingCourses;

    /**
     * Courses the student is registered for but whose grade has not
     * been finalised yet. Each entry contains {@code "code"}
     */
    private List<String> incompleteCourses;
}