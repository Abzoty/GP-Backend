package com.gp.GP_backend.domain.space.entity;

import com.gp.GP_backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "space_memberships",
    uniqueConstraints = @UniqueConstraint(
        name = "UQ_Space_User",
        columnNames = {"space_id", "user_id"}
    )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SpaceMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** MEMBER / MODERATOR / OWNER */
    @Column(length = 20)
    @Builder.Default
    private String role = "MEMBER";

    @Column(name = "joined_at", updatable = false)
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();
}
