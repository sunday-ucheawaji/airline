package com.sunday.services.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunday.services.model.OutboxEvent;
import com.sunday.services.model.OutboxStatus;
import com.sunday.services.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Delivers PENDING outbox rows to Kafka and retries on failure, up to {@link #MAX_ATTEMPTS}
 * attempts, after which a row is marked FAILED and left for manual follow-up (the row itself is
 * the dead-letter record — no separate DLT topic needed). Deserializes each row's payload back
 * into the real event object (using the stored event type) before sending it through the same
 * Object-typed {@code KafkaTemplate} a direct send would have used, so the wire format — including
 * the type header existing consumers rely on — is unchanged.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventSweeper {

    private static final int MAX_ATTEMPTS = 5;
    private static final int BATCH_SIZE = 100;
    private static final long SEND_TIMEOUT_SECONDS = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${outbox.sweep-interval-ms:5000}")
    @Transactional
    public void sweep() {
        List<OutboxEvent> pending = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, PageRequest.of(0, BATCH_SIZE));
        for (OutboxEvent event : pending) {
            attemptDelivery(event);
        }
    }

    private void attemptDelivery(OutboxEvent event) {
        try {
            Class<?> eventType = Class.forName(event.getEventType());
            Object payload = objectMapper.readValue(event.getPayload(), eventType);
            kafkaTemplate.send(event.getTopic(), payload).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            event.setStatus(OutboxStatus.SENT);
            event.setLastAttemptAt(Instant.now());
        } catch (ClassNotFoundException e) {
            // Can't possibly succeed on retry — fail immediately rather than burning attempts.
            event.setStatus(OutboxStatus.FAILED);
            event.setLastAttemptAt(Instant.now());
            event.setLastError(e.toString());
            log.error("Outbox event {} names an unknown event type {} — marking failed permanently",
                    event.getId(), event.getEventType(), e);
        } catch (Exception e) {
            event.setAttempts(event.getAttempts() + 1);
            event.setLastAttemptAt(Instant.now());
            event.setLastError(e.toString());
            if (event.getAttempts() >= MAX_ATTEMPTS) {
                event.setStatus(OutboxStatus.FAILED);
                log.error("Outbox event {} for topic {} failed permanently after {} attempts",
                        event.getId(), event.getTopic(), event.getAttempts(), e);
            } else {
                log.warn("Outbox event {} for topic {} failed (attempt {}/{}), will retry",
                        event.getId(), event.getTopic(), event.getAttempts(), MAX_ATTEMPTS, e);
            }
        }
        outboxEventRepository.save(event);
    }
}
