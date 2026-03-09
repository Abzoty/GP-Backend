package com.gp.GP_backend.domain.user.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank
    @Size(max = 150)
    private String fullName;

    private String studentId;
    private Short academicYear;
    private Short currentSemester;
    private String department;
}
