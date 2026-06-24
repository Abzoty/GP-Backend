package com.gp.GP_backend.domain.questionnaire.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Response DTO for questionnaire scoring.
 *
 * Contains raw department scores and normalized scores (summing to 1.0).
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionnaireScoreResponse {
    /**
     * Raw department scores (sum of all answer scores per department).
     */
    private Map<String, Integer> raw;

    /**
     * Normalized scores (each department / total, handling zero case).
     * Departments: AI, Systems, Web, Security
     */
    private Map<String, BigDecimal> normalized;
}