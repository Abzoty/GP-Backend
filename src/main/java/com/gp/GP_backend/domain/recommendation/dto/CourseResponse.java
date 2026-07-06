package com.gp.GP_backend.domain.recommendation.dto;

import lombok.Data;

@Data
public class CourseResponse {
    private String courseCode;
    private String courseName;
    private Double matchScore;
}
