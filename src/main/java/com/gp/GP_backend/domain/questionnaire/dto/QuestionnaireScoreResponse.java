package com.gp.GP_backend.domain.questionnaire.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionnaireScoreResponse {

    private Map<String, Integer> raw;

    private Map<String, BigDecimal> normalized;
}