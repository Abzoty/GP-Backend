package com.gp.GP_backend.domain.questionnaire.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionnaireDisplayResponse {
    private String version;
    private String title;
    private String instructions;
    private Integer totalQuestions;
    private List<QuestionDisplayDto> questions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionDisplayDto {
        private Integer id;
        private String category;
        private String text;
        private List<AnswerDisplayDto> answers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerDisplayDto {
        private String id;
        private String text;
    }
}

