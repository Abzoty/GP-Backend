package com.gp.GP_backend.domain.prediction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Deserialisation target for the HTTP 422 body returned by the Python
 * prediction service when the student's course data is insufficient for
 * model inference.
 *
 * <p>Matches the {@code InsufficientDataErrorBody} Pydantic schema in the
 * Python service:
 * <pre>{@code
 * {
 * "error": "INSUFFICIENT_COURSE_DATA",
 * "message": "Insufficient course data for prediction: ...",
 * "missing_courses": ["CS401", "MATH301"],
 * "incomplete_courses": [{"code": "PHYS201", "missing_fields": ["grade"]}]
 * }
 * }</pre>
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
public class PythonErrorResponse {

    /**
     * Stable machine-readable error token, e.g. "INSUFFICIENT_COURSE_DATA".
     */
    private String error;

    /**
     * Human-readable summary produced by the Python service.
     */
    private String message;

    /**
     * Course codes the model requires that are entirely absent from the
     * student's registrations.
     */
    @JsonProperty("missing_courses")
    private List<String> missingCourses;

    /**
     * Courses that are registered but have a {@code null} grade.
     * Each entry contains {@code "code"} and {@code "missing_fields"}.
     */
    @JsonProperty("incomplete_courses")
    private List<Map<String, Object>> incompleteCourses;
}