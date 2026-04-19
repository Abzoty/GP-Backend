package com.gp.GP_backend.domain.material.entity;

import com.gp.GP_backend.domain.space.entity.Space;
import com.gp.GP_backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A study resource (file or external link) shared inside a {@link Space}.
 *
 * <p>
 * {@code resourceType} stores either the {@link AcceptedFileType} enum name
 * (e.g. {@code "PDF"}, {@code "DOCX"}) for uploaded files, or the literal
 * string {@code "LINK"} for shared external URLs.
 *
 * <p>
 * For file-based materials, {@code url} holds the stored filename on disk
 * (UUID + extension). For link-based materials, {@code url} holds the full
 * external URL provided by the user.
 *
 * <p>
 * {@code linkCount} is a denormalised counter tracking how many users have
 * bookmarked this material. It is incremented / decremented by
 * {@link com.gp.GP_backend.domain.material.service.MaterialService}
 * whenever a {@link MaterialLink} is created or deleted.
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

    /**
     * Discriminator for this material's content.
     * Values: {@link AcceptedFileType} name for uploads (e.g. {@code "PDF"}),
     * or {@code "LINK"} for external URLs.
     */
    @Column(name = "resource_type", length = 30)
    private String resourceType;

    /**
     * For file materials: the filename as stored on disk (UUID + extension).
     * For link materials: the full external URL.
     */
    @Column(length = 1024)
    private String url;

    /** File size in kilobytes; {@code null} for link materials. */
    @Column(name = "file_size_kb")
    private Integer fileSizeKb;

    /** Denormalised count of users who have bookmarked this material. */
    @Column(name = "link_count")
    @Builder.Default
    private Integer linkCount = 0;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Automatically updated by Hibernate on every UPDATE statement. */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}