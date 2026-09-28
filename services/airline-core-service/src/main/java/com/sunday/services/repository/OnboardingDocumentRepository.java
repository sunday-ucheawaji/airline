package com.sunday.services.repository;

import com.sunday.services.enums.DocumentStatus;
import com.sunday.services.model.OnboardingDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OnboardingDocumentRepository extends JpaRepository<OnboardingDocument, Long> {

    List<OnboardingDocument> findByApplicationIdOrderByCreatedAtAsc(Long applicationId);

    Optional<OnboardingDocument> findByIdAndApplicationId(Long id, Long applicationId);

    long countByApplicationId(Long applicationId);

    /** Uploads whose processing never finished (lost event, consumer down): the sweeper republishes them. */
    List<OnboardingDocument> findByStatusAndUpdatedAtBefore(DocumentStatus status, Instant cutoff);
}
