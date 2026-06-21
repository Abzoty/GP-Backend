package com.gp.GP_backend.domain.material.entity;

import com.gp.GP_backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(
    name = "material_links",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_material_link_material_user",
            columnNames = { "material_id", "user_id" })
    },
    indexes = {
        @Index(name = "idx_material_link_user", columnList = "user_id"),
        @Index(name = "idx_material_link_material", columnList = "material_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UNIQUEIDENTIFIER", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "linked_at", updatable = false)
    @Builder.Default
    private LocalDateTime linkedAt = LocalDateTime.now();
}
