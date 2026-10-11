package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.services.enums.DocumentStatus;
import com.sunday.services.model.OnboardingDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OnboardingDocumentRepository extends JpaRepository<OnboardingDocument, UUID> {

    List<OnboardingDocument> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);

    Optional<OnboardingDocument> findByIdAndApplicationId(UUID id, UUID applicationId);

    long countByApplicationId(UUID applicationId);

    /** Uploads whose processing never finished (lost event, consumer down): the sweeper republishes them. */
    List<OnboardingDocument> findByStatusAndUpdatedAtBefore(DocumentStatus status, Instant cutoff);
}
