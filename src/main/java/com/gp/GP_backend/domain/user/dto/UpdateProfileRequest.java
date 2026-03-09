package com.gp.GP_backend.domain.user.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 150)
    private String fullName;

    private Short academicYear;
    private Short currentSemester;

    @DecimalMin("0.0") @DecimalMax("4.0")
    private BigDecimal gpa;

    private String department;
    private String imageUrl;

    @Size(max = 500)
    private String bio;
}
// Note: add the missing import at the top:
// import jakarta.validation.constraints.DecimalMax;
// import jakarta.validation.constraints.DecimalMin;
