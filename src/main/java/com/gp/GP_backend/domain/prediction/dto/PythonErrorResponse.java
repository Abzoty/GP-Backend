package com.gp.GP_backend.domain.prediction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class PythonErrorResponse {
    private String error;
    private String message;

    @JsonProperty("missing_courses")
    private List<String> missingCourses;

    @JsonProperty("incomplete_courses")
    private List<String> incompleteCourses;
}