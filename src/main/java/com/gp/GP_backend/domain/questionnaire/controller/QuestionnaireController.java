package com.gp.GP_backend.domain.questionnaire.controller;

import com.gp.GP_backend.domain.questionnaire.dto.QuestionnaireAnswersRequest;
import com.gp.GP_backend.domain.questionnaire.dto.QuestionnaireScoreResponse;
import com.gp.GP_backend.domain.questionnaire.service.QuestionnaireService;
import com.gp.GP_backend.domain.questionnaire.service.QuestionnaireScoringService;
import com.gp.GP_backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for questionnaire endpoints.
 */
@RestController
@RequestMapping("/api/v1/questionnaire")
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireController {

    private final QuestionnaireService questionnaireService;
    private final QuestionnaireScoringService qScoringService;

    /**
     * Retrieve the raw questionnaire JSON file as is.
     *
     * @return 200 OK with the exact contents of questionnaire.json
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getQuestionnaire() {
        // Returns the raw String directly, bypassing Jackson serialization
        return ResponseEntity.ok(questionnaireService.getRawQuestionnaire());
    }

    /**
     * Score questionnaire answers.
     *
     * @param request answers map (question ID -> answer choice e.g., {1: "a", 2:
     *                "c"})
     * @return 200 OK with raw and normalized scores (probabilities)
     */
    @PostMapping("/score")
    public ResponseEntity<ApiResponse<QuestionnaireScoreResponse>> scoreQuestionnaire(
            @Valid @RequestBody QuestionnaireAnswersRequest request) {

        QuestionnaireScoreResponse response = qScoringService.scoreAnswers(request);
        return ResponseEntity.ok(ApiResponse.ok("Questionnaire scored", response));
    }
}