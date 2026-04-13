package com.gp.GP_backend.domain.material.entity;

import com.gp.GP_backend.domain.space.entity.Space;
import com.gp.GP_backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A study resource (PDF, link, image, or video) shared inside a {@link Space}.
 *
 * <p>
 * {@code linkCount} is a denormalized counter tracking how many other users
 * have saved ("linked") this material to their personal collection.
 * It's incremented by
 * {@link com.gp.GP_backend.domain.material.service.MaterialService}
 * whenever a {@link MaterialLink} is created.
 */
@Entity
@Table(name = "materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UNIQUEIDENTIFIER", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 1000)
    private String description;

    /** PDF / LINK / IMAGE / VIDEO */
    @Column(name = "resource_type", length = 30)
    private String resourceType;

    @Column(length = 1024)
    private String url;

    /** File size in kilobytes; null for external links. */
    @Column(name = "file_size_kb")
    private Integer fileSizeKb;

    /** How many users have saved this material to their collection. */
    @Column(name = "link_count")
    @Builder.Default
    private Integer linkCount = 0;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
