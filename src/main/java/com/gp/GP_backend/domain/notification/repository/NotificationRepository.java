package com.gp.GP_backend.domain.notification.repository;

import com.gp.GP_backend.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Repository for {@link Notification} entities.
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** All notifications for a user, newest first, paginated. */
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    /** Count of unread notifications — displayed as a badge in the UI. */
    long countByRecipientIdAndIsReadFalse(UUID recipientId);

    /**
     * Marks all unread notifications for a user as read in a single bulk UPDATE.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :userId AND n.isRead = false")
    void markAllAsReadForUser(@Param("userId") UUID userId);
}
