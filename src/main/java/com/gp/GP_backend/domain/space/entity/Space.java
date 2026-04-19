package com.gp.GP_backend.domain.space.entity;

import com.gp.GP_backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The {@code category} field is an enum ({@link SpaceCategory}) that also
 * drives the duplicate-detection logic during creation:
 * {@link SpaceCategory#COLLEGE_COURSE} spaces are matched by
 * {@code courseCode};
 * all other categories use text-similarity on {@code name + description}.
 */
@Entity
@Table(name = "spaces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Space {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UNIQUEIDENTIFIER", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * URL-safe version of the name (e.g. "data-structures-cs301").
     * Generated via {@link com.gp.GP_backend.shared.util.SlugUtil}.
     */
    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private SpaceCategory category;

    @Column(name = "course_code", length = 30)
    private String courseCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "member_count")
    @Builder.Default
    private Integer memberCount = 0;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}