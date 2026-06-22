package com.gp.GP_backend.domain.questionnaire.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Manages questionnaire data.
 */
@Service
@Slf4j
public class QuestionnaireService {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    private QuestionnaireData questionnaire;
    private String rawQuestionnaireJson; // Changed from JsonNode to String

    public QuestionnaireService(ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        loadQuestionnaire();
    }

    private void loadQuestionnaire() {
        try {
            String path = "classpath:reference-data/questionnaire.json";
            try (InputStream is = resourceLoader.getResource(path).getInputStream()) {
                // 1. Read the file exactly as a raw String to serve "as is"
                rawQuestionnaireJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                // 2. Map to Java objects for internal scoring logic
                questionnaire = objectMapper.readValue(rawQuestionnaireJson, QuestionnaireData.class);

                log.info("Questionnaire loaded: version={}, questions={}",
                        questionnaire.metadata.version, questionnaire.metadata.totalQuestions);
            }
        } catch (IOException e) {
            log.error("Failed to load questionnaire", e);
            throw new RuntimeException("Questionnaire initialization failed", e);
        }
    }

    /**
     * Returns the raw JSON content of the questionnaire as a String.
     */
    public String getRawQuestionnaire() {
        return rawQuestionnaireJson;
    }

    /**
     * Returns the mapped questionnaire data (for internal scoring use).
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
        public List<String> departments; // Captures the 5 departments from JSON

        @JsonProperty("total_questions")
        public Integer totalQuestions;

        @JsonProperty("time_estimate")
        public String timeEstimate;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuestionData {
        public Integer id;

        @JsonProperty("type")
        public String category;

        public String text;
        public List<AnswerData> answers;

        public String explanation;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnswerData {
        // Using Object to safely handle both String ("a") and Integer (1) IDs from JSON
        public Object id;
        public String text;

        @JsonProperty("scores")
        public Map<String, Integer> departmentScores;

        public String getIdAsString() {
            return id != null ? id.toString() : null;
        }
    }
}