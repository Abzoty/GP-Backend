package com.gp.GP_backend.domain.questionnaire.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Manages questionnaire data.
 *
 * Loads questionnaire JSON from classpath and provides methods
 * to retrieve questions (without scores/explanation).
 *
 * @since 1.0
 */
@Service
@Slf4j
public class QuestionnaireService {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    private QuestionnaireData questionnaire;

    public QuestionnaireService(ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        loadQuestionnaire();
    }

    /**
     * Loads questionnaire.json from classpath.
     */
    private void loadQuestionnaire() {
        try {
            String path = "classpath:reference-data/questionnaire.json";
            try (InputStream is = resourceLoader.getResource(path).getInputStream()) {
                questionnaire = objectMapper.readValue(is, QuestionnaireData.class);
                log.info("Questionnaire loaded: version={}, questions={}",
                        questionnaire.metadata.version, questionnaire.metadata.totalQuestions);
            }
        } catch (IOException e) {
            log.error("Failed to load questionnaire", e);
            throw new RuntimeException("Questionnaire initialization failed", e);
        }
    }

    /**
     * Returns questionnaire metadata and questions without scores.
     *
     * @return questionnaire with questions (scores removed)
     */
    public QuestionnaireDisplayData getQuestionnaire() {
        // Strip scores from questions for display
        List<QuestionDisplayData> displayQuestions = new ArrayList<>();
        for (QuestionData question : questionnaire.questions) {
            QuestionDisplayData displayQuestion = new QuestionDisplayData();
            displayQuestion.id = question.id;
            displayQuestion.category = question.category;
            displayQuestion.text = question.text;

            // Only include answer text, not department scores
            displayQuestion.answers = new ArrayList<>();
            for (AnswerData answer : question.answers) {
                AnswerDisplayData displayAnswer = new AnswerDisplayData();
                displayAnswer.id = answer.id;
                displayAnswer.text = answer.text;
                displayQuestion.answers.add(displayAnswer);
            }
            displayQuestions.add(displayQuestion);
        }

        QuestionnaireDisplayData display = new QuestionnaireDisplayData();
        display.version = questionnaire.metadata.version;
        display.title = questionnaire.metadata.title;
        display.instructions = questionnaire.metadata.instructions;
        display.totalQuestions = questionnaire.metadata.totalQuestions;
        display.questions = displayQuestions;
        return display;
    }

    /**
     * Returns the full questionnaire data (including scores).
     * For internal use only.
     */
    public QuestionnaireData getFullQuestionnaire() {
        return questionnaire;
    }

    // ==================== Inner DTOs ====================

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuestionnaireData {
        public MetadataData metadata;
        public List<QuestionData> questions;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetadataData {
        public String version;
        public String title;
        public String instructions;

        @JsonProperty("total_questions")
        public Integer totalQuestions;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuestionData {
        public Integer id;

        @JsonProperty("type")
        public String category;

        public String text;
        public List<AnswerData> answers;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnswerData {
        public String id;
        public String text;

        @JsonProperty("scores")
        public Map<String, Integer> departmentScores;
    }

    public static class QuestionnaireDisplayData {
        public String version;
        public String title;
        public String instructions;
        public Integer totalQuestions;
        public List<QuestionDisplayData> questions;
    }

    public static class QuestionDisplayData {
        public Integer id;
        public String category;
        public String text;
        public List<AnswerDisplayData> answers;
    }

    public static class AnswerDisplayData {
        public String id;
        public String text;
    }
}