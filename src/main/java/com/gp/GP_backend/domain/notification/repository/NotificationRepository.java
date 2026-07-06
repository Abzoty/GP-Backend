package com.gp.GP_backend.domain.notification.repository;

import com.gp.GP_backend.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;


public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** All notifications for a user, newest first, paginated. */
    @Query("SELECT n FROM Notification n " +
        "LEFT JOIN FETCH n.sender " +
        "LEFT JOIN FETCH n.recipient " +
        "WHERE n.recipient.id = :userId " +
        "ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(@Param("userId") UUID recipientId, Pageable pageable);

    /** Count of unread notifications — displayed as a badge in the UI. */
    long countByRecipientIdAndIsReadFalse(UUID recipientId);

    /**
     * Marks all unread notifications for a user as read in a single bulk UPDATE.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :userId AND n.isRead = false")
    void markAllAsReadForUser(@Param("userId") UUID userId);

    /**
     * Marks a specific notification as read.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :notificationId AND n.recipient.id = :userId")
    int markNotificationAsReadForUserAndNotificationId(@Param("userId") UUID userId,
            @Param("notificationId") UUID notificationId);
    
}
