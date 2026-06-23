package com.gp.GP_backend.domain.prediction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for the department prediction endpoint.
 *
 * Designed to be consumed directly by chart libraries (Recharts, Chart.js,
 * etc.)
 * without any client-side transformation.
 *
 * <pre>
 * {
 *   "departmentScores": [                   ← sorted by combinedScore DESC
 *     {
 *       "department":        "AI",
 *       "questionnaireScore": 0.3500,        ← always present
 *       "modelScore":         0.4200,        ← null when modelAvailable = false
 *       "combinedScore":      0.3850         ← always present
 *     },
 *     ...
 *   ],
 *   "topDepartment":  "AI",
 *   "modelAvailable": true,
 *   "modelVersion":   "v1.0",               ← null when modelAvailable = false
 *   "warning":        null,                  ← populated when model is down
 *   "weights": {
 *     "questionnaire": 0.5,
 *     "model":         0.5
 *   }
 * }
 * </pre>
 *
 * Frontend chart recipe:
 * <ul>
 * <li>Bar / radar chart: map {@code departmentScores} → three data series
 * (questionnaireScore, modelScore, combinedScore).</li>
 * <li>Recommendation banner: use {@code topDepartment}.</li>
 * <li>Hide the model series and its legend entry when
 * {@code modelAvailable = false}.</li>
 * <li>Display {@code warning} in a dismissible banner when non-null.</li>
 * </ul>
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PredictionResponse {

    /**
     * Per-department score breakdown, sorted by {@code combinedScore} descending.
     * The first element is always the top-recommended department.
     */
    private List<DepartmentScore> departmentScores;

    /**
     * Name of the highest-ranked department (first in {@code departmentScores}).
     */
    private String topDepartment;

    /**
     * Whether the ML model was reachable and returned valid probabilities.
     * When {@code false}: {@code modelScore} is null in every entry, the
     * {@code combinedScore} equals {@code questionnaireScore}, and a
     * {@code warning} is set.
     */
    private boolean modelAvailable;

    /**
     * Version string of the ML model that produced the prediction (e.g. "v1.0").
     * Null when {@code modelAvailable = false}.
     */
    private String modelVersion;

    /**
     * Human-readable warning message, populated only when the model is
     * unavailable or when data is insufficient for a reliable prediction.
     * Null under normal conditions.
     */
    private String warning;

    /**
     * Weights used to compute {@code combinedScore}.
     * Both values are 0.5 when the model is available;
     * questionnaire = 1.0, model = 0.0 when the model is down.
     */
    private PredictionWeights weights;

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scores for a single department across all three measurement sources.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DepartmentScore {

        /**
         * Department name, e.g. "AI", "Systems", "Web", "Security", "CS".
         * Matches the keys in the questionnaire JSON and the Python service response.
         */
        private String department;

        /**
         * Normalized score from the questionnaire (0.0000–1.0000, 4 decimal places).
         * Always present.
         */
        private BigDecimal questionnaireScore;

        /**
         * Probability from the ML model (0.0000–1.0000, 4 decimal places).
         * Null when {@code modelAvailable = false} — use this to conditionally
         * render the model series in the chart.
         */
        private BigDecimal modelScore;

        /**
         * Weighted combination of the two scores (0.0000–1.0000, 4 decimal places).
         * Equals {@code questionnaireScore} when the model is unavailable.
         * Always present — use this as the primary recommendation signal.
         */
        private BigDecimal combinedScore;
    }

    /**
     * Weights used to produce the combined scores.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PredictionWeights {

        /**
         * Fraction of the combined score drawn from questionnaire results (0–1).
         */
        private BigDecimal questionnaire;

        /**
         * Fraction of the combined score drawn from ML model results (0–1).
         */
        private BigDecimal model;
    }
}