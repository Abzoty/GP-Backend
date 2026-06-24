package com.gp.GP_backend.domain.questionnaire.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for submitting questionnaire answers.
 *
 * Contains a map of question ID -> answer ID.
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public
class QuestionnaireAnswersRequest {
    /**
     * Map of question ID (integer) to answer ID (string).
     * Example: {1: "1A", 2: "2C", ...}
     */
    public Map<Integer, String> answers;
}
