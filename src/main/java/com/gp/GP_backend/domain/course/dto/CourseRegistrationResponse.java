package com.gp.GP_backend.domain.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRegistrationResponse {


    private Long id;

    private String code;

    private BigDecimal termWork;

    private BigDecimal examWork;

    private BigDecimal result;

    private String grade;

    private BigDecimal points;

    private Boolean closed;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}