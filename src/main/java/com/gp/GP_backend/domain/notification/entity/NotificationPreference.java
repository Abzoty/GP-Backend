package com.gp.GP_backend.domain.notification.entity;

import java.util.UUID;

import com.gp.GP_backend.domain.user.entity.User;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "notification_preferences",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UNIQUEIDENTIFIER", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "in_app")
    @Builder.Default
    private Boolean inApp = true;

    @Column(name = "email")
    @Builder.Default
    private Boolean email = true;
}