package com.gp.GP_backend.domain.notification.controller;

import com.gp.GP_backend.domain.notification.dto.NotificationResponse;
import com.gp.GP_backend.domain.notification.service.NotificationService;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.response.ApiResponse;
import com.gp.GP_backend.shared.response.PagedResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController controller;

    @Test
    void getNotificationsShouldReturnPagedEnvelope() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        NotificationResponse notification = new NotificationResponse();
        Page<NotificationResponse> page = new PageImpl<>(List.of(notification));

        when(notificationService.getNotificationsForUser(userId, 0, 10)).thenReturn(page);

        ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> entity = controller.getNotifications(user, 0, 10);

        assertThat(entity.getStatusCode().value()).isEqualTo(200);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().isSuccess()).isTrue();
        assertThat(entity.getBody().getData().getContent()).hasSize(1);

        verify(notificationService).getNotificationsForUser(userId, 0, 10);
    }

    @Test
    void markNotificationAsReadShouldDelegateToService() {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        User user = user(userId);

        ResponseEntity<ApiResponse<Void>> entity = controller.markNotificationAsRead(user, notificationId);

        assertThat(entity.getStatusCode().value()).isEqualTo(200);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().isSuccess()).isTrue();
        verify(notificationService).markNotificationAsRead(userId, notificationId);
    }

    @Test
    void toggleEmailNotificationAvailabilityShouldDelegateToService() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);

        ResponseEntity<ApiResponse<Void>> entity = controller.toggleEmailNotificationAvailability(user);

        assertThat(entity.getStatusCode().value()).isEqualTo(200);
        verify(notificationService).toggleEmailNotifications(userId);
    }

    @Test
    void toggleAppNotificationAvailabilityShouldDelegateToService() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);

        ResponseEntity<ApiResponse<Void>> entity = controller.toggleAppNotificationAvailability(user);

        assertThat(entity.getStatusCode().value()).isEqualTo(200);
        verify(notificationService).toggleInAppNotifications(userId);
    }

    private static User user(UUID id) {
        return User.builder()
                .id(id)
                .email("student@example.com")
                .passwordHash("hash")
                .fullName("Student Name")
                .build();
    }
}