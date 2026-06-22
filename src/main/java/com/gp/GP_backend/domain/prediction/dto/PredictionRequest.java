package com.gp.GP_backend.domain.prediction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for department prediction.
 *
 * Contains:
 * - questionnaire answers (map of question ID -> answer ID)
 * - course registrations (list of course codes)
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionRequest {
    /**
     * Questionnaire answers (question ID -> answer ID).
     */
    private Map<Integer, String> questionnaireAnswers;

    /**
     * List of course codes the user has registered for.
     */
    private List<String> courseCodesForPrediction;
}

