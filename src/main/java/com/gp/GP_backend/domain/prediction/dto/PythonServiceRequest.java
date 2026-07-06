package com.gp.GP_backend.domain.prediction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PythonServiceRequest {

    private List<CourseDataDto> courses;
}