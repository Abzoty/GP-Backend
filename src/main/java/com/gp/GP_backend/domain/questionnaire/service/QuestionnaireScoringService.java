package com.gp.GP_backend.domain.questionnaire.service;

import com.gp.GP_backend.domain.questionnaire.dto.QuestionnaireAnswersRequest;
import com.gp.GP_backend.domain.questionnaire.dto.QuestionnaireScoreResponse;
import com.gp.GP_backend.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Scores and validates questionnaire responses.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireScoringService {

    private final QuestionnaireService questionnaireService;

    public QuestionnaireScoreResponse scoreAnswers(QuestionnaireAnswersRequest request) {
        QuestionnaireService.QuestionnaireData questionnaire = questionnaireService.getFullQuestionnaire();

        // 1. Validate all questions are answered
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

        // 2. Initialize raw scores dynamically for all 5 departments defined in
        // metadata
        Map<String, Integer> rawScores = new LinkedHashMap<>();
        if (questionnaire.metadata.departments != null) {
            for (String dept : questionnaire.metadata.departments) {
                rawScores.put(dept, 0);
            }
        } else {
            // Fallback if metadata is missing departments
            rawScores.put("AI", 0);
            rawScores.put("CS", 0);
            rawScores.put("IT", 0);
            rawScores.put("IS", 0);
            rawScores.put("DS", 0);
        }

        // 3. Calculate scores
        for (QuestionnaireService.QuestionData question : questionnaire.questions) {
            String providedAnswerId = request.answers.get(question.id);
            if (providedAnswerId == null)
                continue;

            // Try direct match first (e.g., matching "a" to "a", or "3" to 3)
            QuestionnaireService.AnswerData answer = question.answers.stream()
                    .filter(a -> a.getIdAsString() != null && a.getIdAsString().equals(providedAnswerId))
                    .findFirst()
                    .orElse(null);

            // Fallback: Map letter choices ("a", "b", "c"...) to array indices
            // This handles cases like sending {2: "c"} for a Likert question with IDs
            // 1,2,3,4,5
            if (answer == null) {
                int index = -1;
                switch (providedAnswerId.toLowerCase()) {
                    case "a":
                        index = 0;
                        break;
                    case "b":
                        index = 1;
                        break;
                    case "c":
                        index = 2;
                        break;
                    case "d":
                        index = 3;
                        break;
                    case "e":
                        index = 4;
                        break;
                }
                if (index >= 0 && index < question.answers.size()) {
                    answer = question.answers.get(index);
                }
            }

            if (answer == null) {
                log.warn("Invalid answer ID: {} for question {}", providedAnswerId, question.id);
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid answer ID '" + providedAnswerId + "' for question " + question.id,
                        "INVALID_ANSWER");
            }

            // Accumulate scores for each department
            for (Map.Entry<String, Integer> entry : answer.departmentScores.entrySet()) {
                if (rawScores.containsKey(entry.getKey())) {
                    rawScores.put(entry.getKey(), rawScores.get(entry.getKey()) + entry.getValue());
                }
            }
        }

        // 4. Normalize scores to final probabilities
        Map<String, BigDecimal> normalizedScores = normalizeScores(rawScores);

        QuestionnaireScoreResponse response = new QuestionnaireScoreResponse();
        response.setRaw(rawScores);
        response.setNormalized(normalizedScores);

        log.info("Questionnaire scored: raw={}, probabilities={}", rawScores, normalizedScores);

        return response;
    }

    /**
     * Normalizes department scores so they sum to 1.0 (probabilities).
     */
    private Map<String, BigDecimal> normalizeScores(Map<String, Integer> rawScores) {
        int total = rawScores.values().stream().mapToInt(Integer::intValue).sum();
        Map<String, BigDecimal> normalized = new LinkedHashMap<>();
        int deptCount = rawScores.size();

        if (total == 0) {
            // Edge case: All scores are 0, distribute equally
            BigDecimal equalWeight = BigDecimal.ONE.divide(BigDecimal.valueOf(deptCount), 4, RoundingMode.HALF_UP);
            for (String dept : rawScores.keySet()) {
                normalized.put(dept, equalWeight);
            }
        } else {
            // Standard case: divide each department's score by the total sum
            for (Map.Entry<String, Integer> entry : rawScores.entrySet()) {
                BigDecimal score = BigDecimal.valueOf(entry.getValue())
                        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
                normalized.put(entry.getKey(), score);
            }
        }

        return normalized;
    }
}