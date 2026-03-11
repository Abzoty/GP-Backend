package com.gp.GP_backend.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Creates and manages in-app notifications.
 *
 * TODO: Implement:
 * - notifyNewAnswer(Post post, Answer answer, User answerer)
 * - notifyAnswerAccepted(Answer answer)
 * - notifyUpvoteReceived(Answer answer, User voter)
 * - getNotificationsForUser(UUID userId, Pageable) — paginated
 * - markAllRead(UUID userId)
 * - getUnreadCount(UUID userId)
 *
 * This service should be called by PostService, VoteService, etc. after the
 * primary action completes. It runs within the same transaction by default.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {
    // TODO: inject NotificationRepository
}
