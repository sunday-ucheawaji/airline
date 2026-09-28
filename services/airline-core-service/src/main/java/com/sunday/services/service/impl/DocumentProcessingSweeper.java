package com.sunday.services.service.impl;

import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.config.DocumentProperties;
import com.sunday.services.enums.DocumentStatus;
import com.sunday.services.mapper.DocumentMapper;
import com.sunday.services.model.OnboardingDocument;
import com.sunday.services.repository.OnboardingDocumentRepository;
import com.sunday.services.storage.DocumentStorage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Safety net for the asynchronous pipeline: an upload that is still PROCESSING long after it was saved lost its event
 * (Kafka was down, the consumer was down). Republishing is safe because processing is idempotent. After too many
 * attempts the document is blocked and its quarantined file removed instead of being retried forever.
 */
@Component
@RequiredArgsConstructor
public class DocumentProcessingSweeper {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingSweeper.class);

    private final OnboardingDocumentRepository documentRepository;
    private final OnboardingDocumentEventPublisher publisher;
    private final DocumentStorage storage;
    private final DocumentProperties properties;

    @Scheduled(fixedDelayString = "${airline.documents.sweep-interval:5m}")
    @Transactional
    public void sweep() {
        Instant cutoff = Instant.now().minus(properties.getStaleAfter());
        List<OnboardingDocument> stale = documentRepository.findByStatusAndUpdatedAtBefore(DocumentStatus.PROCESSING, cutoff);
        for (OnboardingDocument document : stale) {
            if (document.getProcessingAttempts() >= properties.getMaxProcessingAttempts()) {
                giveUp(document);
            } else {
                document.setProcessingAttempts(document.getProcessingAttempts() + 1);
                documentRepository.save(document);
                log.info("Republishing stalled document {} (attempt {})", document.getId(), document.getProcessingAttempts());
                publisher.publish(DocumentMapper.toUploadedEvent(document));
            }
        }
    }

    private void giveUp(OnboardingDocument document) {
        log.warn("Blocking document {} after {} failed processing attempts", document.getId(), document.getProcessingAttempts());
        document.setStatus(DocumentStatus.BLOCKED);
        document.setBlockedReason(ErrorMessageUtil.DOCUMENT_BLOCKED_PROCESSING_FAILED);
        documentRepository.save(document);
        try {
            storage.deleteQuarantine(document.getStorageKey());
        } catch (RuntimeException e) {
            log.warn("Could not remove quarantined file of document {}", document.getId(), e);
        }
    }
}
