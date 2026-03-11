package com.gp.GP_backend.domain.recommendation.dto;

import lombok.Data;

/**
 * A recommended course returned by the ML service.
 * TODO: Add fields once the ML API contract is finalised.
 */
@Data
public class CourseResponse {
    private String courseCode;
    private String courseName;
    private Double matchScore;
}
