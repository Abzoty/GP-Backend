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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orchestrates the full department-prediction pipeline.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionOrchestrationService {

        private static final BigDecimal QUESTIONNAIRE_WEIGHT = new BigDecimal("0.5");
        private static final BigDecimal MODEL_WEIGHT = new BigDecimal("0.5");
        private static final int SCORE_SCALE = 4;

        // ── Department Name Mapping ────────────────────────────────────────────────
        // Maps abbreviations to their canonical full names to prevent duplicates
        // when combining questionnaire scores and ML model probabilities.
        private static final Map<String, String> DEPARTMENT_NAME_MAP = Map.of(
                        "CS", "Computer Science",
                        "IS", "Information Systems",
                        "IT", "Information Technology",
                        "AI", "Artificial Intelligence",
                        "DS", "Operation Research & Decision Support");

        private final PredictionServiceClient predictionServiceClient;
        private final CourseRegistrationRepository courseRegistrationRepository;

        // ──────────────────────────────────────────────────────────────────────────

        /**
         * Run the prediction pipeline.
         */
        public PredictionResponse predict(User user, PredictionRequest request) {
                log.info("Starting prediction pipeline for user={}", user.getId());

                // ── Step 1: Normalize questionnaire scores to full department names ───
                Map<String, BigDecimal> questionnaireScores = normalizeDepartmentKeys(request.getNormalizedScores());
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

                // Normalize model scores to full department names as well
                Map<String, BigDecimal> modelScores = modelAvailable
                                ? normalizeDepartmentKeys(pythonResponse.getProbabilities())
                                : null;
                String modelVersion = modelAvailable ? pythonResponse.getModelVersion() : null;

                log.info("Python service — available={}, probabilities={}", modelAvailable, modelScores);

                // ── Step 4: Determine effective weights ───────────────────────────────
                BigDecimal qWeight = modelAvailable ? QUESTIONNAIRE_WEIGHT : BigDecimal.ONE;
                BigDecimal mWeight = modelAvailable ? MODEL_WEIGHT : BigDecimal.ZERO;

                // ── Step 5: Build per-department score list ───────────────────────────
                // Now that both maps use full names, the Set will naturally prevent duplicates.
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

        /**
         * Normalizes department keys (e.g., "CS" -> "Computer Science").
         * If a key is already a full name or not in the map, it remains unchanged.
         */
        private Map<String, BigDecimal> normalizeDepartmentKeys(Map<String, BigDecimal> scores) {
                if (scores == null || scores.isEmpty()) {
                        return scores;
                }
                Map<String, BigDecimal> normalized = new LinkedHashMap<>();
                for (Map.Entry<String, BigDecimal> entry : scores.entrySet()) {
                        // Map abbreviation to full name, or keep original if not found
                        String fullName = DEPARTMENT_NAME_MAP.getOrDefault(entry.getKey(), entry.getKey());

                        // Merge scores in case both abbreviation and full name were somehow present
                        normalized.merge(fullName, entry.getValue(), BigDecimal::add);
                }
                return normalized;
        }

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