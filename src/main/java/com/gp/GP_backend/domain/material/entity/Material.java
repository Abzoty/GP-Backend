package com.gp.GP_backend.domain.material.entity;

import com.gp.GP_backend.domain.space.entity.Space;
import com.gp.GP_backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "materials", indexes = {
        @Index(name = "idx_materials_space_created", columnList = "space_id, created_at DESC"),
        @Index(name = "idx_materials_space_type", columnList = "space_id, resource_type")
})
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

    @Column(name = "resource_type", length = 30)
    private String resourceType;

    /**
     * For file materials: the filename as stored on disk (UUID + extension).
     * For link materials: the full external URL.
     */
    @Column(length = 1024)
    private String url;

    /** File size in kilobytes;  null for link materials. */
    @Column(name = "file_size_kb")
    private Integer fileSizeKb;

    @Column(name = "link_count")
    @Builder.Default
    private Integer linkCount = 0;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}