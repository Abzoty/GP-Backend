package com.gp.GP_backend.domain.prediction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class PythonServiceResponse {

    private Map<String, BigDecimal> probabilities;

    @JsonProperty("model_version")
    private String modelVersion;
}