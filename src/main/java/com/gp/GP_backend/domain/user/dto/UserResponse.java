package com.gp.GP_backend.domain.user.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;


@Data
public class UserResponse {

    private UUID id;
    private String email;
    private String fullName;
    private String studentId;
    private Integer academicYear;
    private Integer currentSemester;
    private BigDecimal gpa;
    private String department;
    private String imageUrl;
    private String bio;
}
