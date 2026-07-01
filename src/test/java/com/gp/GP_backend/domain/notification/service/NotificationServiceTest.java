package com.gp.GP_backend.domain.notification.service;

import com.gp.GP_backend.domain.notification.dto.NotificationResponse;
import com.gp.GP_backend.domain.notification.entity.Notification;
import com.gp.GP_backend.domain.notification.entity.NotificationPreference;
import com.gp.GP_backend.domain.notification.entity.NotificationType;
import com.gp.GP_backend.domain.notification.entity.ReferenceType;
import com.gp.GP_backend.domain.notification.repository.NotificationPreferencesRepository;
import com.gp.GP_backend.domain.notification.repository.NotificationRepository;
import com.gp.GP_backend.domain.post.repository.AnswerRepository;
import com.gp.GP_backend.domain.post.repository.PostRepository;
import com.gp.GP_backend.domain.space.repository.SpaceMembershipRepository;
import com.gp.GP_backend.domain.space.repository.SpaceRepository;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.repository.UserRepository;
import com.gp.GP_backend.shared.util.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private SpaceMembershipRepository spaceMembershipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationPreferencesRepository notificationPreferencesRepository;

    @InjectMocks
    private NotificationService service;

    @Test
    void getNotificationsForUserShouldMapPagedNotifications() {
        UUID userId = UUID.randomUUID();
        User recipient = user(userId, "recipient@example.com", "Recipient");
        User sender = user(UUID.randomUUID(), "sender@example.com", "Sender");
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .recipient(recipient)
                .sender(sender)
                .notificationType(NotificationType.IN_APP.name())
                .title("Hello")
                .message("World")
                .referenceType(ReferenceType.NEW_POST.name())
                .referenceId(UUID.randomUUID())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.isActiveById(userId)).thenReturn(true);
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, org.springframework.data.domain.PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(notification)));

        Page<NotificationResponse> page = service.getNotificationsForUser(userId, 0, 10);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getSenderName()).isEqualTo("Sender");
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Hello");

        verify(userRepository).isActiveById(userId);
        verify(notificationRepository).findByRecipientIdOrderByCreatedAtDesc(userId, org.springframework.data.domain.PageRequest.of(0, 10));
    }

    @Test
    void markNotificationAsReadShouldReturnTrueWhenRowIsUpdated() {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        when(userRepository.isActiveById(userId)).thenReturn(true);
        when(notificationRepository.markNotificationAsReadForUserAndNotificationId(userId, notificationId)).thenReturn(1);

        boolean updated = service.markNotificationAsRead(userId, notificationId);

        assertThat(updated).isTrue();
        verify(notificationRepository).markNotificationAsReadForUserAndNotificationId(userId, notificationId);
    }

    @Test
    void toggleInAppNotificationsShouldCreateDefaultPreferenceWhenMissing() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "student@example.com", "Student");

        when(userRepository.isActiveById(userId)).thenReturn(true);
        when(notificationPreferencesRepository.findPreferenceByUserId(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        boolean toggled = service.toggleInAppNotifications(userId);

        assertThat(toggled).isTrue();

        ArgumentCaptor<NotificationPreference> captor = ArgumentCaptor.forClass(NotificationPreference.class);
        verify(notificationPreferencesRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getInApp()).isFalse();
        assertThat(captor.getValue().getEmail()).isTrue();
    }

    @Test
    void toggleEmailNotificationsShouldDelegateToRepositoryWhenPreferenceExists() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "student@example.com", "Student");
        NotificationPreference preference = NotificationPreference.builder()
                .user(user)
                .inApp(true)
                .email(true)
                .build();

        when(userRepository.isActiveById(userId)).thenReturn(true);
        when(notificationPreferencesRepository.findPreferenceByUserId(userId)).thenReturn(Optional.of(preference));
        when(notificationPreferencesRepository.toggleEmailNotifications(userId)).thenReturn(1);

        boolean toggled = service.toggleEmailNotifications(userId);

        assertThat(toggled).isTrue();
        verify(notificationPreferencesRepository).toggleEmailNotifications(userId);
    }

    private static User user(UUID id, String email, String name) {
        return User.builder()
                .id(id)
                .email(email)
                .passwordHash("hash")
                .fullName(name)
                .build();
    }
}