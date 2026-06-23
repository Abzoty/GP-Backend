package com.gp.GP_backend.domain.prediction.service;

import com.gp.GP_backend.domain.course.entity.CourseRegistration;
import com.gp.GP_backend.domain.course.repository.CourseRegistrationRepository;
import com.gp.GP_backend.domain.prediction.dto.CourseDataDto;
import com.gp.GP_backend.domain.prediction.dto.PredictionRequest;
import com.gp.GP_backend.domain.prediction.dto.PredictionResponse;
import com.gp.GP_backend.domain.prediction.dto.PythonServiceRequest;
import com.gp.GP_backend.domain.prediction.dto.PythonServiceResponse;
import com.gp.GP_backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orchestrates the full department-prediction pipeline.
 *
 * Pipeline steps:
 * 1. Accept the pre-computed normalized questionnaire scores from the request.
 * 2. Load all of the authenticated user's course registrations from the DB.
 * 3. POST the course data to the Python ML service.
 * 4. Combine questionnaire scores + model probabilities (50 / 50 by default).
 * 5. Return a chart-ready PredictionResponse sorted by combined score.
 *
 * Graceful degradation: if the Python service is unreachable the response still
 * returns 200 with modelAvailable=false and combinedScore ==
 * questionnaireScore.
 *
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionOrchestrationService {

        private static final BigDecimal QUESTIONNAIRE_WEIGHT = new BigDecimal("0.5");
        private static final BigDecimal MODEL_WEIGHT = new BigDecimal("0.5");
        private static final int SCORE_SCALE = 4;

        private final PredictionServiceClient predictionServiceClient;
        private final CourseRegistrationRepository courseRegistrationRepository;

        // ──────────────────────────────────────────────────────────────────────────

        /**
         * Run the prediction pipeline.
         *
         * @param user    authenticated user — courses are loaded automatically
         * @param request contains the normalized scores from the /score endpoint
         * @return structured response with per-department breakdowns
         */
        public PredictionResponse predict(User user, PredictionRequest request) {
                log.info("Starting prediction pipeline for user={}", user.getId());

                // ── Step 1: Use the normalized scores the client already computed ─────
                Map<String, BigDecimal> questionnaireScores = request.getNormalizedScores();
                log.debug("Questionnaire normalized scores received: {}", questionnaireScores);

                // ── Step 2: Fetch all course registrations for this user ──────────────
                List<CourseRegistration> courses = courseRegistrationRepository.findByUser(user);
                log.info("Loaded {} course registration(s) for user={}", courses.size(), user.getId());

                // ── Step 3: Call the Python ML service ────────────────────────────────
                List<CourseDataDto> courseData = courses.stream()
                                .map(this::toCourseDataDto)
                                .collect(Collectors.toList());

                PythonServiceResponse pythonResponse = predictionServiceClient.predict(
                                PythonServiceRequest.builder().courses(courseData).build());

                boolean modelAvailable = pythonResponse != null
                                && pythonResponse.getProbabilities() != null
                                && !pythonResponse.getProbabilities().isEmpty();

                Map<String, BigDecimal> modelScores = modelAvailable ? pythonResponse.getProbabilities() : null;
                String modelVersion = modelAvailable ? pythonResponse.getModelVersion() : null;

                log.info("Python service — available={}, probabilities={}", modelAvailable, modelScores);

                // ── Step 4: Determine effective weights ───────────────────────────────
                BigDecimal qWeight = modelAvailable ? QUESTIONNAIRE_WEIGHT : BigDecimal.ONE;
                BigDecimal mWeight = modelAvailable ? MODEL_WEIGHT : BigDecimal.ZERO;

                // ── Step 5: Build per-department score list ───────────────────────────
                // Questionnaire keys define the canonical department set;
                // any extra keys the model returns are appended after.
                Set<String> allDepartments = new LinkedHashSet<>(questionnaireScores.keySet());
                if (modelAvailable) {
                        allDepartments.addAll(modelScores.keySet());
                }

                List<PredictionResponse.DepartmentScore> departmentScores = allDepartments.stream()
                                .map(dept -> buildDepartmentScore(
                                                dept, questionnaireScores, modelScores, modelAvailable, qWeight,
                                                mWeight))
                                .sorted(Comparator.comparing(
                                                PredictionResponse.DepartmentScore::getCombinedScore,
                                                Comparator.reverseOrder()))
                                .collect(Collectors.toList());

                // ── Step 6: Assemble response ─────────────────────────────────────────
                String topDepartment = departmentScores.isEmpty()
                                ? "Unknown"
                                : departmentScores.get(0).getDepartment();

                PredictionResponse response = PredictionResponse.builder()
                                .departmentScores(departmentScores)
                                .topDepartment(topDepartment)
                                .modelAvailable(modelAvailable)
                                .modelVersion(modelVersion)
                                .weights(PredictionResponse.PredictionWeights.builder()
                                                .questionnaire(qWeight)
                                                .model(mWeight)
                                                .build())
                                .build();

                if (!modelAvailable) {
                        response.setWarning(
                                        "The ML model is currently unavailable. " +
                                                        "Recommendation is based on questionnaire scores only.");
                }

                log.info("Prediction complete — topDepartment={}, modelAvailable={}",
                                topDepartment, modelAvailable);

                return response;
        }

        // ──────────────────────────────────────────────────────────────────────────
        // Private helpers
        // ──────────────────────────────────────────────────────────────────────────

        private PredictionResponse.DepartmentScore buildDepartmentScore(
                        String dept,
                        Map<String, BigDecimal> questionnaireScores,
                        Map<String, BigDecimal> modelScores,
                        boolean modelAvailable,
                        BigDecimal qWeight,
                        BigDecimal mWeight) {

                BigDecimal qScore = questionnaireScores
                                .getOrDefault(dept, BigDecimal.ZERO)
                                .setScale(SCORE_SCALE, RoundingMode.HALF_UP);

                BigDecimal mScore = null;
                BigDecimal combinedScore;

                if (modelAvailable && modelScores != null) {
                        mScore = modelScores
                                        .getOrDefault(dept, BigDecimal.ZERO)
                                        .setScale(SCORE_SCALE, RoundingMode.HALF_UP);

                        combinedScore = qScore.multiply(qWeight)
                                        .add(mScore.multiply(mWeight))
                                        .setScale(SCORE_SCALE, RoundingMode.HALF_UP);
                } else {
                        combinedScore = qScore;
                }

                return PredictionResponse.DepartmentScore.builder()
                                .department(dept)
                                .questionnaireScore(qScore)
                                .modelScore(mScore)
                                .combinedScore(combinedScore)
                                .build();
        }

        private CourseDataDto toCourseDataDto(CourseRegistration cr) {
                return CourseDataDto.builder()
                                .code(cr.getCode())
                                .termWork(cr.getTermWork())
                                .examWork(cr.getExamWork())
                                .result(cr.getResult())
                                .grade(cr.getGrade())
                                .points(cr.getPoints())
                                .build();
        }
}