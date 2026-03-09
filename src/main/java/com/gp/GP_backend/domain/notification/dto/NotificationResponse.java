package com.gp.GP_backend.domain.notification.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private Long senderId;
    private String senderName;
    private String notificationType;
    private String title;
    private String message;
    private String referenceType;
    private Long referenceId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
