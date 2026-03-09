package com.gp.GP_backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "student_id", unique = true, length = 50)
    private String studentId;

    @Column(name = "academic_year")
    private Short academicYear;           // 1–5

    @Column(name = "current_semester")
    private Short currentSemester;        // 1–10

    @Column(precision = 4, scale = 2)
    private BigDecimal gpa;

    @Column(length = 100)
    private String department;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(length = 500)
    private String bio;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
