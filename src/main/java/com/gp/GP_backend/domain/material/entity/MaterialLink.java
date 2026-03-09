package com.gp.GP_backend.domain.material.entity;

import com.gp.GP_backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "material_links")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MaterialLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
