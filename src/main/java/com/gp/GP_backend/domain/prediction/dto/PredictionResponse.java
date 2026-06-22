package com.gp.GP_backend.domain.prediction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Response DTO for department prediction.
 *
 * Contains:
 * - Department probabilities (from model or fixed)
 * - Questionnaire scores (for transparency)
 * - Model availability and version
 * - Top recommended department
 * - Any warnings
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionResponse {
    /**
     * Department probabilities/scores from the prediction model.
     * Keys: AI, Systems, Web, Security
     */
    private Map<String, BigDecimal> departments;

    /**
     * Model metadata (name, version).
     */
    private ModelInfo model;

    /**
     * Questionnaire normalized scores (for reference).
     */
    private Map<String, BigDecimal> questionnaire;

    /**
     * Combined score (weighted average of model predictions and questionnaire
     * scores).
     */
    private Map<String, BigDecimal> combined;

    /**
     * Top recommended department.
     */
    private String topDepartment;

    /**
     * Whether a model was available for prediction.
     * If false, departments are stub/fixed values and a warning is included.
     */
    private Boolean modelAvailable;

    /**
     * Optional warning message if model was unavailable or other issues occurred.
     */
    private String warning;

    /**
     * Model version string (e.g., "v1.0").
     */
    private String modelVersion;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ModelInfo {
        private String name;
        private String version;
    }
}