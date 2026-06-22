package com.gp.GP_backend.domain.questionnaire.service;

import com.gp.GP_backend.domain.questionnaire.dto.QuestionnaireAnswersRequest;
import com.gp.GP_backend.domain.questionnaire.dto.QuestionnaireScoreResponse;
import com.gp.GP_backend.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Scores and validates questionnaire responses.
 *
 * Responsibilities:
 * - Validate all 20 questions are answered
 * - Validate answer IDs are valid for their questions
 * - Calculate raw department scores
 * - Normalize scores (sum = 1.0 per department, handle zero case)
 * - Return both raw and normalized scores
 *
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireScoringService {

    private final QuestionnaireService questionnaireService;

    /**
     * Scores a set of questionnaire answers.
     *
     * Validates:
     * - All 20 questions are answered
     * - Each answer ID is valid for its question
     *
     * Calculates:
     * - Raw department scores (sum of answer scores per department)
     * - Normalized scores (each department / total, handling zero case)
     *
     * @param request the answers request
     * @return scoring response with raw and normalized scores
     * @throws ApiException 400 INCOMPLETE_QUESTIONNAIRE if questions missing
     * @throws ApiException 400 INVALID_ANSWER if answer ID is invalid
     */
    public QuestionnaireScoreResponse scoreAnswers(QuestionnaireAnswersRequest request) {
        QuestionnaireService.QuestionnaireData questionnaire = questionnaireService.getFullQuestionnaire();

        // Validate all questions are answered
        Set<Integer> providedQuestionIds = request.answers.keySet();
        Set<Integer> requiredQuestionIds = questionnaire.questions.stream()
                .map(q -> q.id)
                .collect(Collectors.toSet());

        Set<Integer> missingQuestionIds = new HashSet<>(requiredQuestionIds);
        missingQuestionIds.removeAll(providedQuestionIds);

        if (!missingQuestionIds.isEmpty()) {
            String missingStr = missingQuestionIds.stream()
                    .sorted()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            log.warn("Incomplete questionnaire: missing questions {}", missingStr);
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Incomplete questionnaire. Missing answers for questions: " + missingStr,
                    "INCOMPLETE_QUESTIONNAIRE");
        }

        // Validate answer IDs and collect scores
        Map<String, Integer> rawScores = new HashMap<>();
        rawScores.put("AI", 0);
        rawScores.put("Systems", 0);
        rawScores.put("Web", 0);
        rawScores.put("Security", 0);

        for (QuestionnaireService.QuestionData question : questionnaire.questions) {
            String providedAnswerId = request.answers.get(question.id);
            if (providedAnswerId == null) {
                continue; // Already validated above
            }

            // Find the answer in this question
            QuestionnaireService.AnswerData answer = question.answers.stream()
                    .filter(a -> a.id.equals(providedAnswerId))
                    .findFirst()
                    .orElse(null);

            if (answer == null) {
                log.warn("Invalid answer ID: {} for question {}", providedAnswerId, question.id);
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid answer ID '" + providedAnswerId + "' for question " + question.id,
                        "INVALID_ANSWER");
            }

            // Accumulate scores
            for (Map.Entry<String, Integer> entry : answer.departmentScores.entrySet()) {
                rawScores.put(entry.getKey(), rawScores.get(entry.getKey()) + entry.getValue());
            }
        }

        // Normalize scores
        Map<String, BigDecimal> normalizedScores = normalizeScores(rawScores);

        QuestionnaireScoreResponse response = new QuestionnaireScoreResponse();
        response.setRaw(rawScores);
        response.setNormalized(normalizedScores);

        log.info("Questionnaire scored: raw={}, normalized={}", rawScores, normalizedScores);

        return response;
    }

    /**
     * Normalizes department scores so they sum to 1.0.
     *
     * Handles edge case: if all scores are 0, returns equal weights (0.25 each for
     * 4 departments).
     *
     * @param rawScores map of department -> raw score
     * @return map of department -> normalized score (BigDecimal)
     */
    private Map<String, BigDecimal> normalizeScores(Map<String, Integer> rawScores) {
        int total = rawScores.values().stream().mapToInt(Integer::intValue).sum();

        Map<String, BigDecimal> normalized = new HashMap<>();

        if (total == 0) {
            // All scores are 0: equal weights
            BigDecimal equalWeight = BigDecimal.ONE.divide(BigDecimal.valueOf(4), 4, java.math.RoundingMode.HALF_UP);
            for (String dept : rawScores.keySet()) {
                normalized.put(dept, equalWeight);
            }
        } else {
            // Normal case: divide each by total
            for (Map.Entry<String, Integer> entry : rawScores.entrySet()) {
                BigDecimal score = BigDecimal.valueOf(entry.getValue())
                        .divide(BigDecimal.valueOf(total), 4, java.math.RoundingMode.HALF_UP);
                normalized.put(entry.getKey(), score);
            }
        }

        return normalized;
    }
}