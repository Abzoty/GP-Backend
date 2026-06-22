package com.gp.GP_backend.domain.questionnaire.controller;

import com.gp.GP_backend.domain.questionnaire.dto.QuestionnaireAnswersRequest;
import com.gp.GP_backend.domain.questionnaire.dto.QuestionnaireDisplayResponse;
import com.gp.GP_backend.domain.questionnaire.dto.QuestionnaireScoreResponse;
import com.gp.GP_backend.domain.questionnaire.service.QuestionnaireService;
import com.gp.GP_backend.domain.questionnaire.service.QuestionnaireScoringService;
import com.gp.GP_backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for questionnaire endpoints.
 *
 * All endpoints are public (no JWT required).
 *
 * Paths:
 * - GET /api/v1/questionnaire : retrieve questionnaire (questions without
 * scores)
 * - POST /api/v1/questionnaire/score : submit answers and receive department
 * scores
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/questionnaire")
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireController {

    private final QuestionnaireService questionnaireService;
    private final QuestionnaireScoringService qScoringService;
    private final ModelMapper modelMapper;

    /**
     * Retrieve the questionnaire.
     *
     * Returns all 20 questions with their answer options (without department
     * scores).
     *
     * @return 200 OK with questionnaire metadata and questions
     */
    @GetMapping
    public ResponseEntity<ApiResponse<QuestionnaireDisplayResponse>> getQuestionnaire() {
        QuestionnaireService.QuestionnaireDisplayData displayData = questionnaireService.getQuestionnaire();
        QuestionnaireDisplayResponse response = modelMapper.map(displayData, QuestionnaireDisplayResponse.class);
        return ResponseEntity.ok(ApiResponse.ok("Questionnaire retrieved", response));
    }

    /**
     * Score questionnaire answers.
     *
     * Validates:
     * - All 20 questions are answered
     * - Each answer ID is valid
     *
     * Returns:
     * - Raw department scores (sum of individual answer scores)
     * - Normalized scores (each department / total)
     *
     * @param request answers map (question ID -> answer ID)
     * @return 200 OK with raw and normalized scores
     */
    @PostMapping("/score")
    public ResponseEntity<ApiResponse<QuestionnaireScoreResponse>> scoreQuestionnaire(
            @Valid @RequestBody QuestionnaireAnswersRequest request) {

        QuestionnaireScoreResponse response = qScoringService.scoreAnswers(request);
        return ResponseEntity.ok(ApiResponse.ok("Questionnaire scored", response));
    }
}