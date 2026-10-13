package com.sunday.services.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunday.services.model.OutboxEvent;
import com.sunday.services.model.OutboxStatus;
import com.sunday.services.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Enqueues an event for later delivery instead of sending it to Kafka directly. {@code enqueue}
 * does nothing but a repository {@code save} — it joins the caller's own transaction (default
 * {@code REQUIRED} propagation), so the outbox row commits or rolls back atomically with whatever
 * business change the caller just made. {@link OutboxEventSweeper} does the actual Kafka send,
 * independently and with retry.
 */
@Service
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void enqueue(String topic, Object event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event for topic " + topic, e);
        }

        outboxEventRepository.save(OutboxEvent.builder()
                .topic(topic)
                .eventType(event.getClass().getName())
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .attempts(0)
                .createdAt(Instant.now())
                .build());
    }
}
