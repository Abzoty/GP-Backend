package com.gp.GP_backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.gp.GP_backend.domain.user.dto.UpdateProfileRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UNIQUEIDENTIFIER", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "student_id", unique = true, length = 50)
    private String studentId;

    @Column(name = "academic_year")
    private Integer academicYear;

    @Column(name = "current_semester")
    private Integer currentSemester;

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

    // ─── UserDetails contract ──────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.isActive);
    }

    // ─── Lean logic for applying profile updates ─────────────────────────
    public void applyPatch(UpdateProfileRequest request) {
    Optional.ofNullable(request.getFullName()).ifPresent(this::setFullName);
    Optional.ofNullable(request.getAcademicYear()).ifPresent(this::setAcademicYear);
    Optional.ofNullable(request.getCurrentSemester()).ifPresent(this::setCurrentSemester);
    Optional.ofNullable(request.getDepartment()).ifPresent(this::setDepartment);
    Optional.ofNullable(request.getImageUrl()).ifPresent(this::setImageUrl);
    Optional.ofNullable(request.getBio()).ifPresent(this::setBio);
}
}
