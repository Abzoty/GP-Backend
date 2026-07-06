package com.gp.GP_backend.domain.course.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRegistrationRequest {

    @NotBlank(message = "Course code is required")
    @Size(max = 20, message = "Course code must not exceed 20 characters")
    private String code;


    @DecimalMin(value = "0", inclusive = true, message = "Term work must be at least 0")
    @DecimalMax(value = "40", inclusive = true, message = "Term work must not exceed 40")
    private BigDecimal termWork;


    @DecimalMin(value = "0", inclusive = true, message = "Exam work must be at least 0")
    @DecimalMax(value = "60", inclusive = true, message = "Exam work must not exceed 60")
    private BigDecimal examWork;
}