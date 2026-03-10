package com.gp.GP_backend.domain.user.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String studentId;
    private Short academicYear;
    private Short currentSemester;
    private BigDecimal gpa;
    private String department;
    private String imageUrl;
    private String bio;
}