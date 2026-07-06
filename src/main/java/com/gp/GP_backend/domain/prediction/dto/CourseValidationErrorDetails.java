package com.gp.GP_backend.domain.prediction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseValidationErrorDetails {

    @Builder.Default
    private String error = "INSUFFICIENT_COURSE_DATA";

    private List<String> missingCourses;

    private List<String> incompleteCourses;
}