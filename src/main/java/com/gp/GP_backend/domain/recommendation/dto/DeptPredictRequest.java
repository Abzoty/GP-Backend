package com.gp.GP_backend.domain.recommendation.dto;

import lombok.Data;
import java.util.List;

/**
 * Payload sent to the ML service for department prediction.
 * TODO: Define fields once the ML API contract is finalised.
 */
@Data
public class DeptPredictRequest {
    private List<String> completedCourseCodes;
    private Double gpa;
    private String interests;
}
