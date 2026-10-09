package com.sunday.service.service;

import com.sunday.service.channel.NotificationChannelRegistry;
import com.sunday.service.model.Notification;
import com.sunday.service.model.NotificationChannel;
import com.sunday.service.model.NotificationEventType;
import com.sunday.service.model.NotificationStatus;
import com.sunday.service.repository.NotificationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * The one place a notification actually gets sent. One row per (eventType, reference, channel) is
 * both the delivery record AND the idempotency claim — closing the two biggest gaps this service had
 * (no dedup, no tracking) with one table.
 *
 * On failure {@link #send} RETHROWS, on purpose — unlike the listener it replaces, which caught and
 * swallowed every send exception, silently defeating the Kafka-level retry/DLT config that exists
 * specifically for this failure mode. A redelivery after a rethrow finds the row still PENDING/FAILED
 * (never SENT) and legitimately retries; a row already SENT is a true duplicate and is skipped.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchService {

    private final NotificationRepository notificationRepository;
    private final NotificationChannelRegistry channelRegistry;
    private final NotificationClaimService claimService;
    private final MeterRegistry meterRegistry;

    public void dispatch(NotificationEventType eventType, String reference, NotificationChannel channel,
                          String recipient, String subject, String body) {
        if (recipient == null || recipient.isBlank()) {
            log.debug("No {} recipient for {} {}, skipping", channel, eventType, reference);
            return;
        }
        if (!channelRegistry.resolve(channel).isAvailable()) {
            log.debug("{} channel disabled, skipping {} {}", channel, eventType, reference);
            return;
        }

        MDC.put("notificationReference", reference);
        MDC.put("notificationChannel", channel.name());
        try {
            Notification record = claimService.claim(eventType, reference, channel, recipient, subject, body);
            if (record == null) {
                log.info("{} {} via {} already sent, skipping redelivery", eventType, reference, channel);
                return;
            }
            send(record);
        } finally {
            MDC.remove("notificationReference");
            MDC.remove("notificationChannel");
        }
    }

    // Deliberately no explicit @Transactional here: exactly one repository.save() happens per call
    // (either the SENT path or the FAILED path, never both), so Spring Data's own per-method
    // transaction on that single save is sufficient — there's nothing a wrapping transaction would
    // need to roll back together, and rethrowing after the save must NOT undo it.
    private void send(Notification record) {
        try {
            String providerMessageId = channelRegistry.resolve(record.getChannel())
                    .send(record.getRecipient(), record.getSubject(), record.getBody());
            record.setStatus(NotificationStatus.SENT);
            record.setProviderMessageId(providerMessageId);
            record.setSentAt(LocalDateTime.now());
            record.setFailureReason(null);
            notificationRepository.save(record);
            meterRegistry.counter("notification.sent", "channel", record.getChannel().name(),
                    "eventType", record.getEventType().name()).increment();
            log.info("{} sent for {} {} (attempt {})", record.getChannel(), record.getEventType(),
                    record.getReference(), record.getAttemptCount());
        } catch (Exception e) {
            record.setStatus(NotificationStatus.FAILED);
            record.setFailureReason(truncate(e.getMessage()));
            notificationRepository.save(record);
            meterRegistry.counter("notification.failed", "channel", record.getChannel().name(),
                    "eventType", record.getEventType().name()).increment();
            log.error("{} failed for {} {} (attempt {}): {}", record.getChannel(), record.getEventType(),
                    record.getReference(), record.getAttemptCount(), e.getMessage(), e);
            throw new NotificationDeliveryException(record.getChannel().name(), e);
        }
    }

    /** Explicit, permission-gated resend of a specific record — bypasses the "already SENT" skip on
     *  purpose, reusing the subject/body snapshotted at the original send time. */
    public Notification resend(Notification record) {
        record.setAttemptCount(record.getAttemptCount() + 1);
        send(record);
        return record;
    }

    private static String truncate(String message) {
        if (message == null) return null;
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
