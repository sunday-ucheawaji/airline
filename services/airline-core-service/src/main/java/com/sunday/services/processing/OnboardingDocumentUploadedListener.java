package com.sunday.services.processing;

import com.sunday.common_lib.event.OnboardingDocumentUploadedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Receives "an upload is waiting in quarantine". If processing throws (storage or scanner outage) the error handler
 * retries with back-off and finally parks the event on the dead-letter topic.
 */
@Component
@RequiredArgsConstructor
public class OnboardingDocumentUploadedListener {

    private final DocumentProcessor processor;

    @KafkaListener(topics = OnboardingDocumentUploadedEvent.TOPIC)
    public void onUploaded(OnboardingDocumentUploadedEvent event) {
        processor.process(event);
    }
}
