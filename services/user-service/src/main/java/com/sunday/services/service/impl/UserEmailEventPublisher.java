package com.sunday.services.service.impl;

import com.sunday.common_lib.event.EmailVerificationRequestedEvent;
import com.sunday.common_lib.event.PasswordResetRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Hands verification and reset emails to notification-service, only after the transaction that stored the token
 * has committed. A failed send is logged, not retried: the user can ask for another email.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEmailEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationRequested(EmailVerificationRequestedEvent event) {
        send(EmailVerificationRequestedEvent.TOPIC, event.getUserId().toString(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        send(PasswordResetRequestedEvent.TOPIC, event.getUserId().toString(), event);
    }

    private void send(String topic, String key, Object event) {
        try {
            kafkaTemplate.send(topic, key, event).whenComplete((result, failure) -> {
                if (failure != null) {
                    log.warn("Could not publish {} for user {}", topic, key, failure);
                }
            });
        } catch (RuntimeException e) {
            log.warn("Could not publish {} for user {}", topic, key, e);
        }
    }
}
