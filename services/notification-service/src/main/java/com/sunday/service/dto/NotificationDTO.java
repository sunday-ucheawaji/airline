package com.sunday.service.dto;

import java.util.UUID;
import com.sunday.service.model.NotificationChannel;
import com.sunday.service.model.NotificationEventType;
import com.sunday.service.model.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Local to notification-service — no other service consumes a notification record today. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private UUID id;
    private NotificationEventType eventType;
    private String reference;
    private NotificationChannel channel;
    private String recipient;
    private NotificationStatus status;
    private String providerMessageId;
    private String failureReason;
    private int attemptCount;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}
