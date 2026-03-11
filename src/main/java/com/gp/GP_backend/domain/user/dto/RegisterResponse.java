package com.gp.GP_backend.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data @Builder @AllArgsConstructor
public class RegisterResponse {
    private Long userId;
    private String email;
    private String fullName;
}