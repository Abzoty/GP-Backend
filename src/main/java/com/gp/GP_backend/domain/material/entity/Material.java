package com.gp.GP_backend.domain.material.entity;

import com.gp.GP_backend.domain.space.entity.Space;
import com.gp.GP_backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "materials")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "file_size_kb")
    private Integer fileSizeKb;

    @Column(name = "link_count")
    @Builder.Default
    private Integer linkCount = 0;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
