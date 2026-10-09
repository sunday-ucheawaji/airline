package com.sunday.service.dto;

import com.sunday.service.model.Notification;

public class NotificationMapper {

    private NotificationMapper() {}

    public static NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .eventType(n.getEventType())
                .reference(n.getReference())
                .channel(n.getChannel())
                .recipient(n.getRecipient())
                .status(n.getStatus())
                .providerMessageId(n.getProviderMessageId())
                .failureReason(n.getFailureReason())
                .attemptCount(n.getAttemptCount())
                .createdAt(n.getCreatedAt())
                .sentAt(n.getSentAt())
                .build();
    }
}
