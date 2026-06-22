package com.gp.GP_backend.domain.prediction.service;

import com.gp.GP_backend.domain.prediction.dto.PredictionRequest;
import com.gp.GP_backend.domain.prediction.dto.PredictionResponse;
import com.gp.GP_backend.domain.questionnaire.dto.QuestionnaireAnswersRequest;
import com.gp.GP_backend.domain.questionnaire.dto.QuestionnaireScoreResponse;
import com.gp.GP_backend.domain.questionnaire.service.QuestionnaireScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;   

/**
 * Orchestrates the department prediction workflow.
 *
 * Steps:
 * 1. Score the questionnaire answers
 * 2. Call the prediction service with course codes
 * 3. Combine questionnaire scores and model probabilities
 * 4. Determine top department
 * 5. Return comprehensive prediction response
 *
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionOrchestrationService {

    private final QuestionnaireScoringService questionnaireScoringService;
    private final PredictionServiceClient predictionServiceClient;

    /**
     * Orchestrates prediction for a user.
     *
     * @param request prediction request (questionnaire answers + courses)
     * @return prediction response with department probabilities
     */
    public PredictionResponse predict(PredictionRequest request) {
        log.info("Starting prediction orchestration: courses={}",
                request.getCourseCodesForPrediction());

        // Step 1: Score the questionnaire
        QuestionnaireAnswersRequest answersRequest = new QuestionnaireAnswersRequest();
        answersRequest.answers = request.getQuestionnaireAnswers();
        QuestionnaireScoreResponse scoreResponse = questionnaireScoringService.scoreAnswers(answersRequest);

        // Step 2: Get model predictions
        Map<String, BigDecimal> modelPredictions = predictionServiceClient.predict(
                request.getCourseCodesForPrediction());
        boolean modelAvailable = modelPredictions != null && !modelPredictions.isEmpty();

        // Step 3: Combine scores
        Map<String, BigDecimal> combined = combineScores(
                scoreResponse.getNormalized(),
                modelPredictions,
                modelAvailable);

        // Step 4: Find top department
        String topDepartment = findTopDepartment(combined);

        // Build response
        PredictionResponse response = PredictionResponse.builder()
                .departments(modelPredictions)
                .questionnaire(scoreResponse.getNormalized())
                .combined(combined)
                .topDepartment(topDepartment)
                .modelAvailable(modelAvailable)
                .modelVersion("v1.0")
                .model(PredictionResponse.ModelInfo.builder()
                        .name("Department Prediction Model")
                        .version("1.0")
                        .build())
                .build();

        if (!modelAvailable) {
            response.setWarning("Prediction model is currently unavailable. " +
                    "Showing questionnaire-based recommendation only.");
        }

        log.info("Prediction complete: top={}, modelAvailable={}", topDepartment, modelAvailable);

        return response;
    }

    /**
     * Combines questionnaire scores and model predictions.
     *
     * If model is available: weighted average (50% questionnaire, 50% model).
     * If model is unavailable: use questionnaire scores only.
     *
     * @param questionnaireScores normalized questionnaire scores
     * @param modelPredictions    model probabilities
     * @param modelAvailable      whether model is available
     * @return combined scores
     */
    private Map<String, BigDecimal> combineScores(
            Map<String, BigDecimal> questionnaireScores,
            Map<String, BigDecimal> modelPredictions,
            boolean modelAvailable) {

        Map<String, BigDecimal> combined = new HashMap<>();

        if (!modelAvailable || modelPredictions == null || modelPredictions.isEmpty()) {
            // Model unavailable: use questionnaire only
            combined.putAll(questionnaireScores);
        } else {
            // Model available: weighted average
            BigDecimal weight = BigDecimal.valueOf(0.5);
            for (String dept : questionnaireScores.keySet()) {
                BigDecimal qScore = questionnaireScores.getOrDefault(dept, BigDecimal.ZERO);
                BigDecimal mScore = modelPredictions.getOrDefault(dept, BigDecimal.ZERO);

                BigDecimal combinedScore = qScore.multiply(weight)
                        .add(mScore.multiply(weight));

                combined.put(dept, combinedScore.setScale(4, java.math.RoundingMode.HALF_UP));
            }
        }

        return combined;
    }

    /**
     * Finds the top department from combined scores.
     *
     * @param combined combined department scores
     * @return name of top department
     */
    private String findTopDepartment(Map<String, BigDecimal> combined) {
        return combined.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Web"); // Default fallback
    }
}