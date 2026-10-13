package com.sunday.services.repository;

import com.sunday.services.model.OnboardingDocumentAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OnboardingDocumentAccessRepository extends JpaRepository<OnboardingDocumentAccess, UUID> {

    List<OnboardingDocumentAccess> findByDocumentIdOrderByAccessedAtAsc(UUID documentId);
}
