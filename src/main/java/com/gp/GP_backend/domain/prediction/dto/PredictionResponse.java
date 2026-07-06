package com.gp.GP_backend.domain.prediction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PredictionResponse {

    private List<DepartmentScore> departmentScores;

    private String topDepartment;

    private boolean modelAvailable;

    private String modelVersion;

    private String warning;

    private PredictionWeights weights;

    // ─────────────────────────────────────────────────────────────────────────
    //  Inner DTOs
    // ─────────────────────────────────────────────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DepartmentScore {

        private String department;

        private BigDecimal questionnaireScore;

        private BigDecimal modelScore;

        private BigDecimal combinedScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PredictionWeights {

        private BigDecimal questionnaire;

        private BigDecimal model;
    }
}