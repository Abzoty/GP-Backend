package com.gp.GP_backend.domain.user.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Public-facing user profile data transferred to the client.
 *
 * <p>
 * Sensitive fields ({@code passwordHash}, {@code isActive}) are intentionally
 * excluded. ModelMapper maps {@link com.gp.GP_backend.domain.user.entity.User}
 * to this class by field name (STRICT strategy), so every field here must
 * exactly match a field in the entity.
 */
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
