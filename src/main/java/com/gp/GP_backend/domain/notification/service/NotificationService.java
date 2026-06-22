package com.gp.GP_backend.domain.notification.service;

import com.gp.GP_backend.shared.exception.ApiException;
import com.gp.GP_backend.shared.util.EmailService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.gp.GP_backend.domain.material.entity.Material;
import com.gp.GP_backend.domain.notification.dto.NotificationResponse;
import com.gp.GP_backend.domain.notification.entity.Notification;
import com.gp.GP_backend.domain.notification.entity.NotificationType;
import com.gp.GP_backend.domain.notification.entity.ReferenceType;
import com.gp.GP_backend.domain.notification.repository.NotificationPreferencesRepository;
import com.gp.GP_backend.domain.notification.repository.NotificationRepository;
import com.gp.GP_backend.domain.post.entity.Answer;
import com.gp.GP_backend.domain.post.entity.Post;
import com.gp.GP_backend.domain.post.repository.AnswerRepository;
import com.gp.GP_backend.domain.post.repository.PostRepository;
import com.gp.GP_backend.domain.space.repository.SpaceMembershipRepository;
import com.gp.GP_backend.domain.space.repository.SpaceRepository;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.repository.UserRepository;

/**
 * Creates and manages in-app notifications.
 *
 * This service should be called by PostService, VoteService, etc. after the
 * primary action completes. It runs within the same transaction by default.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SpaceRepository spaceRepository;
    private final SpaceMembershipRepository spaceMembershipRepository;
    private final UserRepository userRepository;
    private final AnswerRepository answerRepository;
    private final PostRepository postRepository;
    private final EmailService emailService;
    private final NotificationPreferencesRepository notificationPreferencesRepository;

    // NotificationPreference lookups may return null when a user has no preference row yet.
    // Treat missing preference as "disabled" to avoid NPEs in notification flows.
    private boolean acceptsInApp(UUID userId) {
        return Boolean.TRUE.equals(notificationPreferencesRepository.isUserAcceptInAppNotifications(userId));
    }

    private boolean acceptsEmail(UUID userId) {
        return Boolean.TRUE.equals(notificationPreferencesRepository.isUserAcceptEmailNotifications(userId));
    }

    public void notifyNewPostCreated(Post post, User author) {
        String spaceName = spaceRepository.findNameById(post.getSpaceId());
        List<UUID> memberIds = spaceMembershipRepository.findMembersIdsBySpaceId(post.getSpaceId());
        
        String title = "New post in your space " + spaceName;
        String body = "A new post has been created by " + author.getFullName() + " in " + spaceName + ". Check it out!";

        // fetch sender once, not inside the loop
        User sender = userRepository.findById(author.getId()).orElseThrow();

        for (UUID recipientId : memberIds) {
            if (!recipientId.equals(author.getId())) {
                User recipient = userRepository.findById(recipientId).orElseThrow();
                notificationRepository.save(
                    Notification.builder()
                        .recipient(recipient)
                        .sender(sender)
                        .title(title)
                        .message(body)
                        .referenceType(ReferenceType.NEW_POST.name())
                        .referenceId(post.getId())
                        .notificationType(NotificationType.IN_APP.name())
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build()
                );
            }
        }
    }


    public void notifyNewMaterialShared(Material newMaterial, User sharer) {
        String spaceName = spaceRepository.findNameById(newMaterial.getSpace().getId());
        List<UUID> memberIds = spaceMembershipRepository.findMembersIdsBySpaceId(newMaterial.getSpace().getId());

        UUID materialId = newMaterial.getId();
        String title = "New material shared in your space " + spaceName;
        String body = "A new material has been shared by " + sharer.getFullName() + " in " + spaceName + ". Check it out!";

        List<UUID> availableMembersIds = notificationPreferencesRepository.findUserIdsThatAcceptInAppNotifications(memberIds);

        for (UUID recipientId : availableMembersIds) {
            if (!recipientId.equals(sharer.getId())) {
                User recipient = userRepository.findById(recipientId).orElseThrow();
                notificationRepository.save(
                    Notification.builder()
                        .recipient(recipient)
                        .sender(sharer)
                        .title(title)
                        .message(body)
                        .referenceType(ReferenceType.NEW_MATERIAL.name())
                        .referenceId(materialId)
                        .notificationType(NotificationType.IN_APP.name())
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build()
                );
            }
        }
    }


    public void notifyUpvoteAnswerReceived(UUID answerId, User voter) {
        Answer answer = answerRepository.findById(answerId).orElseThrow();
        User recipient = userRepository.findById(answer.getAuthorId()).orElseThrow();
        if (!acceptsInApp(recipient.getId())) return;
        String title = "Your answer got an upvote!";
        String body = voter.getFullName() + " upvoted your answer: \"" + answer.getBody().substring(0, Math.min(50, answer.getBody().length())) + "...\"";

        notificationRepository.save(
            Notification.builder()
                .recipient(recipient)
                .sender(voter)
                .title(title)
                .message(body)
                .referenceType(ReferenceType.UPVOTE.name())
                .referenceId(answer.getId())
                .notificationType(NotificationType.IN_APP.name())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build()
        );
    }


    public void notifyGoodQuestionMarked(UUID postId, User marker) {
        Post post = postRepository.findById(postId).orElseThrow();
        User recipient = userRepository.findById(post.getAuthorId()).orElseThrow();
        if (!acceptsInApp(recipient.getId())) return;
        String title = "Good Question!";
        String body = marker.getFullName() + " marked your question: \"" + post.getTitle() + "\" as a Good Question!";

        notificationRepository.save(
            Notification.builder()
                .recipient(recipient)
                .sender(marker)
                .title(title)
                .message(body)
                .referenceType(ReferenceType.GOOD_QUESTION.name())
                .referenceId(post.getId())
                .notificationType(NotificationType.IN_APP.name())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build()
        );
    }


    public void notifyNewAnswer(Post post, Answer answer, User answerer) {
        User recipient = userRepository.findById(post.getAuthorId()).orElseThrow();
        String title = "New answer to your question!";
        String body = answerer.getFullName() + " answered your question: \"" + post.getTitle() + "\". Check it out!";

        if (acceptsInApp(recipient.getId())) {
            notificationRepository.save(
                Notification.builder()
                    .recipient(recipient)
                    .sender(answerer)
                    .title(title)
                    .message(body)
                    .referenceType(ReferenceType.NEW_ANSWER.name())
                    .referenceId(answer.getId())
                    .notificationType(NotificationType.IN_APP_AND_EMAIL.name())
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build()
            );
        }

        // email notification
        if (acceptsEmail(recipient.getId()))
        {
            emailService.sendEmail(
                recipient.getEmail(),
                title,
                body + "\n\nView it here: " + "https://yourapp.com/posts/" + post.getId() // after frontend is done
            );
        }
    }


    public void notifyAnswerAccepted(UUID answerId) {
        Answer answer = answerRepository.findById(answerId).orElseThrow();
        User recipient = userRepository.findById(answer.getAuthorId()).orElseThrow();
        String title = "Your answer was accepted!";
        String body = "Congratulations! Your answer: \"" + answer.getBody().substring(0, Math.min(50, answer.getBody().length())) + "...\" was accepted as the solution.";

        if (acceptsInApp(recipient.getId()))
        {
            notificationRepository.save(
                Notification.builder()
                    .recipient(recipient)
                    .sender(null) // system notification
                    .title(title)
                    .message(body)
                    .referenceType(ReferenceType.ANSWER_ACCEPTED.name())
                    .referenceId(answer.getId())
                    .notificationType(NotificationType.IN_APP_AND_EMAIL.name())
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build()
            );
        }


        // email notification
        if (acceptsEmail(recipient.getId()))
        {
            emailService.sendEmail(
                recipient.getEmail(),
                title,
                body + "\n\nView it here: " + "https://yourapp.com/answers/" + answer.getId() // after frontend is done
            );
        }
    }

    public NotificationResponse mapToNotificationResponse(Notification notification) {
        return NotificationResponse.builder()
            .id(notification.getId())
            .title(notification.getTitle())
            .message(notification.getMessage())
            .senderName(notification.getSender() != null ? notification.getSender().getFullName() : "System")
            .createdAt(notification.getCreatedAt())
            .notificationType(notification.getNotificationType())
            .isRead(notification.getIsRead())
            .senderId(notification.getSender() != null ? notification.getSender().getId() : null)
            .referenceType(notification.getReferenceType())
            .referenceId(notification.getReferenceId())
            .build();
    }


    public Page<NotificationResponse> getNotificationsForUser(UUID userId, int page, int size) {
        boolean isActive = userRepository.isActiveById(userId);
        if (!isActive) throw new ApiException(HttpStatus.NOT_FOUND, "User not found");
        Page<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, Pageable.ofSize(size).withPage(page));
        return notifications.map(this::mapToNotificationResponse);
    }


    @Transactional
    public boolean markNotificationAsRead(UUID userId, UUID notificationId) {
        boolean isActive = userRepository.isActiveById(userId);
        if (!isActive) throw new ApiException(HttpStatus.NOT_FOUND, "User not found");
        return notificationRepository.markNotificationAsReadForUserAndNotificationId(userId, notificationId) > 0;
    }

    @Transactional
    public boolean toggleInAppNotifications(UUID userId) {
        boolean isActive = userRepository.isActiveById(userId);
        if (!isActive) throw new ApiException(HttpStatus.NOT_FOUND, "User not found");
        return notificationPreferencesRepository.toggleInAppNotifications(userId) > 0;
    }

    @Transactional
    public boolean toggleEmailNotifications(UUID userId) {
        boolean isActive = userRepository.isActiveById(userId);
        if (!isActive) throw new ApiException(HttpStatus.NOT_FOUND, "User not found");
        return notificationPreferencesRepository.toggleEmailNotifications(userId) > 0;
    }
}
