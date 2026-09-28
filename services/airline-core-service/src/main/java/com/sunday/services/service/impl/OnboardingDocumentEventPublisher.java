package com.sunday.services.service.impl;

import com.sunday.common_lib.event.OnboardingDocumentUploadedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends "an upload is waiting in quarantine" to Kafka. The event is only published after the database transaction
 * that saved the document has committed, so the processor can never see an event for a row that does not exist.
 * If sending fails the document stays PROCESSING and the sweeper republishes it later.
 */
@Component
@RequiredArgsConstructor
public class OnboardingDocumentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OnboardingDocumentEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUploaded(OnboardingDocumentUploadedEvent event) {
        publish(event);
    }

    public void publish(OnboardingDocumentUploadedEvent event) {
        try {
            kafkaTemplate.send(OnboardingDocumentUploadedEvent.TOPIC, String.valueOf(event.getDocumentId()), event)
                    .whenComplete((result, failure) -> {
                        if (failure != null) {
                            log.warn("Could not publish upload event for document {}; the sweeper will retry", event.getDocumentId(), failure);
                        }
                    });
        } catch (RuntimeException e) {
            log.warn("Could not publish upload event for document {}; the sweeper will retry", event.getDocumentId(), e);
        }
    }
}
