package com.gp.GP_backend.domain.prediction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO representing a single course registration sent to the Python prediction
 * service.
 *
 * Uses snake_case via @JsonProperty so the Python service can consume it
 * without
 * any extra serialisation config on either side.
 *
 * Fields that may be null (pending grades) are included in the payload as JSON
 * null
 * so the Python service can distinguish "no grade yet" from "grade = 0".
 *
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS) // send null fields explicitly
public class CourseDataDto {

    /**
     * Course code (e.g. "CS301").
     * Python uses this for one-hot encoding / category mapping.
     */
    private String code;

    /**
     * Continuous-assessment score (0–40), or null if not yet entered.
     */
    @JsonProperty("term_work")
    private BigDecimal termWork;

    /**
     * Final-exam score (0–60), or null if not yet entered.
     */
    @JsonProperty("exam_work")
    private BigDecimal examWork;

    /**
     * Total result (termWork + examWork, 0–100), or null if pending.
     * Server-derived; Python can recompute if needed.
     */
    private BigDecimal result;

    /**
     * Letter grade ("A+", "A", …, "F"), or null if pending.
     */
    private String grade;

    /**
     * GPA points (0.0–4.0), or null if pending.
     */
    private BigDecimal points;

}