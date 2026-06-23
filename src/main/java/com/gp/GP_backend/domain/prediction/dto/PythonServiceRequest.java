package com.gp.GP_backend.domain.prediction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Payload sent from the Java backend to the Python prediction service's
 * {@code POST /predict} endpoint.
 *
 * The Python service is responsible for all feature engineering
 * (one-hot encoding, GPA averaging, etc.) before running the model.
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PythonServiceRequest {

    /**
     * All course registrations for the current user (open and closed).
     * May be an empty list if the user has not registered any courses yet;
     * the Python service should handle that gracefully.
     */
    private List<CourseDataDto> courses;
}