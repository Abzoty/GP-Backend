package com.gp.GP_backend.domain.notification.repository;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gp.GP_backend.domain.notification.entity.Notification;
import com.gp.GP_backend.domain.notification.entity.NotificationPreference;
import com.gp.GP_backend.domain.user.entity.User;

public interface NotificationPreferencesRepository extends JpaRepository<NotificationPreference, UUID> {

    @Query("SELECT n FROM NotificationPreference n WHERE n.user.id = :userId")
    Optional<NotificationPreference> findPreferenceByUserId(@Param("userId") UUID userId);

    @Query("SELECT n FROM Notification n WHERE n.recipient.id = :userId")
    Notification findByUserId(@Param("userId") UUID userId);

    @Query("SELECT n.inApp FROM NotificationPreference n WHERE n.user.id = :userId")
    Boolean isUserAcceptInAppNotifications(@Param("userId") UUID userId);

    @Query("SELECT n.email FROM NotificationPreference n WHERE n.user.id = :userId")
    Boolean isUserAcceptEmailNotifications(@Param("userId") UUID userId);

    @Query("SELECT n.user.id FROM NotificationPreference n WHERE n.inApp = true AND n.user.id IN :userIds")
    List<UUID> findUserIdsThatAcceptInAppNotifications(@Param("userIds") List<UUID> userIds);

    @Modifying
    @Query("UPDATE NotificationPreference n SET n.inApp = CASE WHEN n.inApp = true THEN false ELSE true END WHERE n.user.id = :userId")
    int toggleInAppNotifications(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE NotificationPreference n SET n.email = CASE WHEN n.email = true THEN false ELSE true END WHERE n.user.id = :userId")
    int toggleEmailNotifications(@Param("userId") UUID userId);

    void save(User saved);
}
