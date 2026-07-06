package com.gp.GP_backend.domain.recommendation.dto;

import lombok.Data;
import java.util.List;


@Data
public class DeptPredictRequest {
    private List<String> completedCourseCodes;
    private Double gpa;
    private String interests;
}
