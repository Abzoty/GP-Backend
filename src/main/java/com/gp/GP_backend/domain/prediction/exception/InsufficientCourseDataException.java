package com.gp.GP_backend.domain.prediction.exception;

import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Thrown when the Python ML service returns HTTP 422 because the student's
 * course registrations are insufficient for model inference.
 *
 * <p>
 * This is a <em>business-validation error</em>, not a connectivity failure.
 * It must therefore:
 * <ul>
 * <li>NOT trigger the Resilience4j circuit-breaker fallback.</li>
 * <li>NOT be treated as "model unavailable" by the orchestration service.</li>
 * <li>Propagate to {@code PredictionController} so the student receives an
 * actionable 422 response listing exactly which courses are missing or
 * still awaiting a final grade.</li>
 * </ul>
 *
 * <p>
 * <strong>Required Resilience4j configuration</strong> — add the following
 * to your {@code application.yml} (or {@code application.properties}) so that
 * this exception bypasses the circuit-breaker fallback entirely:
 *
 * <pre>{@code
 * # application.yml
 * resilience4j:
 *   circuitbreaker:
 *     instances:
 *       prediction-service:
 *         ignore-exceptions:
 *           - com.gp.GP_backend.domain.prediction.exception.InsufficientCourseDataException
 *
 * # application.properties (alternative)
 * resilience4j.circuitbreaker.instances.prediction-service.ignore-exceptions[0]=\
 *   com.gp.GP_backend.domain.prediction.exception.InsufficientCourseDataException
 * }</pre>
 *
 * @since 1.0
 */
@Getter
public class InsufficientCourseDataException extends RuntimeException {

    /**
     * Course codes that appear in the model's feature set but are entirely
     * absent from the student's course registrations.
     */
    private final List<String> missingCourses;

    /**
     * Courses that are registered but whose grade has not been finalised yet.
     * Each entry is a map with keys {@code "code"} (String) and
     * {@code "missing_fields"} (List&lt;String&gt;).
     */
    private final List<Map<String, Object>> incompleteCourses;

    public InsufficientCourseDataException(
            String message,
            List<String> missingCourses,
            List<Map<String, Object>> incompleteCourses) {
        super(message);
        this.missingCourses = missingCourses != null ? missingCourses : List.of();
        this.incompleteCourses = incompleteCourses != null ? incompleteCourses : List.of();
    }
}