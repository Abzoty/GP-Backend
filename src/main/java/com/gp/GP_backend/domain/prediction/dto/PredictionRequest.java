package com.gp.GP_backend.domain.prediction.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class PredictionRequest {

    @NotNull(message = "Normalized scores are required")
    @NotEmpty(message = "Normalized scores cannot be empty")
    private Map<String, BigDecimal> normalizedScores;
}