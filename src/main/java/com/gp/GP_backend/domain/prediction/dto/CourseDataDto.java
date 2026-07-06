package com.gp.GP_backend.domain.prediction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class CourseDataDto {

    private String code;

    @JsonProperty("term_work")
    private BigDecimal termWork;

    @JsonProperty("exam_work")
    private BigDecimal examWork;

    private BigDecimal result;

    private String grade;

    private BigDecimal points;

}