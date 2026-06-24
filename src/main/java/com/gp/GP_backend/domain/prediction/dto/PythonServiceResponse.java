package com.gp.GP_backend.domain.prediction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Response returned by the Python prediction service's {@code POST /predict}
 * endpoint.
 *
 * Example payload the Python service should produce:
 * 
 * <pre>
 * {
 *   "probabilities": {
 *     "AI":       0.4312,
 *     "CS":  0.2581,
 *     "IS":      0.1734,
 *     "IT": 0.0901,
 *     "DS":       0.0472
 *   },
 *   "model_version": "v1.0"
 * }
 * </pre>
 *
 * All probability values must be in [0, 1] and should sum to ~1.0.
 * Department keys must match the keys used by the questionnaire service.
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PythonServiceResponse {

    /**
     * Department-name → probability map from the ML model.
     * Keys must match the department names in the questionnaire JSON
     */
    private Map<String, BigDecimal> probabilities;

    /**
     * Model version string for traceability (e.g. "v1.0").
     */
    @JsonProperty("model_version")
    private String modelVersion;
}