package com.gp.GP_backend.domain.space.entity;

import com.gp.GP_backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Join table that links a {@link User} to a {@link Space} with an associated
 * role.
 *
 * <p>
 * The composite unique constraint {@code UQ_Space_User} prevents a user
 * from joining the same space more than once.
 *
 * <p>
 * Roles (in ascending privilege order):
 * <ul>
 * <li>{@code MEMBER} – read, post, answer, share materials</li>
 * <li>{@code MODERATOR} – all member actions + pin/delete posts</li>
 * <li>{@code OWNER} – all moderator actions + manage members/space
 * settings</li>
 * <li>{@code ADMIN} – full control: edit space details, grant admin to others,
 * and perform all OWNER actions. The space creator is
 * automatically assigned this role on creation.</li>
 * </ul>
 */
@Entity
@Table(name = "space_memberships", uniqueConstraints = @UniqueConstraint(name = "UQ_Space_User", columnNames = {
        "space_id", "user_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UNIQUEIDENTIFIER", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** MEMBER / MODERATOR / OWNER / ADMIN — see class-level Javadoc. */
    @Column(length = 20)
    @Builder.Default
    private String role = "MEMBER";

    @Column(name = "joined_at", updatable = false)
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();
}