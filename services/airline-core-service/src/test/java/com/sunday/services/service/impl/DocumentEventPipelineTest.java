package com.sunday.services.service.impl;

import com.sunday.common_lib.event.OnboardingDocumentUploadedEvent;
import com.sunday.services.config.DocumentProperties;
import com.sunday.services.enums.DocumentStatus;
import com.sunday.services.enums.DocumentType;
import com.sunday.services.model.AirlineOnboardingApplication;
import com.sunday.services.model.OnboardingDocument;
import com.sunday.services.repository.OnboardingDocumentRepository;
import com.sunday.services.storage.InMemoryDocumentStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The Kafka publisher and the sweeper that backs it up. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentEventPipelineTest {

    @Mock KafkaTemplate<String, Object> kafkaTemplate;
    @Mock OnboardingDocumentRepository documentRepository;

    private final InMemoryDocumentStorage storage = new InMemoryDocumentStorage();
    private final DocumentProperties properties = new DocumentProperties();
    private OnboardingDocumentEventPublisher publisher;
    private DocumentProcessingSweeper sweeper;

    @BeforeEach
    void setUp() {
        publisher = new OnboardingDocumentEventPublisher(kafkaTemplate);
        sweeper = new DocumentProcessingSweeper(documentRepository, publisher, storage, properties);
        when(kafkaTemplate.send(any(String.class), any(String.class), any())).thenReturn(new CompletableFuture<>());
    }

    @Test
    void theEventIsSentToTheUploadTopicKeyedByDocumentId() {
        OnboardingDocumentUploadedEvent event = OnboardingDocumentUploadedEvent.builder().documentId(5L).applicationId(10L).build();

        publisher.onUploaded(event);

        verify(kafkaTemplate).send(OnboardingDocumentUploadedEvent.TOPIC, "5", event);
    }

    @Test
    void aBrokerFailureNeverBreaksTheUploadBecauseTheSweeperRetries() {
        when(kafkaTemplate.send(any(String.class), any(String.class), any())).thenThrow(new IllegalStateException("broker down"));

        assertThatCode(() -> publisher.publish(OnboardingDocumentUploadedEvent.builder().documentId(5L).build())).doesNotThrowAnyException();
    }

    @Test
    void aStalledDocumentIsRepublishedAndItsAttemptCountGoesUp() {
        OnboardingDocument stalled = document(3L, 0);
        when(documentRepository.findByStatusAndUpdatedAtBefore(eq(DocumentStatus.PROCESSING), any(Instant.class))).thenReturn(List.of(stalled));

        sweeper.sweep();

        assertThat(stalled.getProcessingAttempts()).isEqualTo(1);
        ArgumentCaptor<Object> sent = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(OnboardingDocumentUploadedEvent.TOPIC), eq("3"), sent.capture());
        assertThat(((OnboardingDocumentUploadedEvent) sent.getValue()).getQuarantineKey()).isEqualTo("onboarding/10/k3");
    }

    @Test
    void onlyDocumentsOlderThanTheStalePeriodAreConsidered() {
        properties.setStaleAfter(Duration.ofMinutes(10));

        sweeper.sweep();

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(documentRepository).findByStatusAndUpdatedAtBefore(eq(DocumentStatus.PROCESSING), cutoff.capture());
        assertThat(cutoff.getValue()).isBefore(Instant.now().minus(Duration.ofMinutes(9)));
    }

    @Test
    void aDocumentThatKeepsFailingIsBlockedAndItsQuarantinedFileRemoved() {
        properties.setMaxProcessingAttempts(3);
        OnboardingDocument hopeless = document(4L, 3);
        storage.putQuarantine(hopeless.getStorageKey(), new ByteArrayInputStream(new byte[]{1}), 1);
        when(documentRepository.findByStatusAndUpdatedAtBefore(eq(DocumentStatus.PROCESSING), any(Instant.class))).thenReturn(List.of(hopeless));

        sweeper.sweep();

        assertThat(hopeless.getStatus()).isEqualTo(DocumentStatus.BLOCKED);
        assertThat(hopeless.getBlockedReason()).contains("several attempts");
        assertThat(storage.quarantine).isEmpty();
        verify(kafkaTemplate, never()).send(any(String.class), any(String.class), any());
    }

    private static OnboardingDocument document(Long id, int attempts) {
        AirlineOnboardingApplication application = new AirlineOnboardingApplication();
        application.setId(10L);
        return OnboardingDocument.builder().id(id).application(application).documentType(DocumentType.OTHER)
                .originalFileName("a.pdf").storageProvider("MEMORY").storageBucket("quarantine")
                .storageKey("onboarding/10/k" + id).uploadedByUserId(1L).processingAttempts(attempts).createdAt(Instant.now()).build();
    }
}
