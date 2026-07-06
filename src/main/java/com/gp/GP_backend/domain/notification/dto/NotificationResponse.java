package com.gp.GP_backend.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private UUID id;
    private UUID senderId;
    private String senderName;
    private String notificationType;
    private String title;
    private String message;
    private String referenceType;
    private UUID referenceId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
