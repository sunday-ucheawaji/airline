package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.services.model.OnboardingCaseOwnerHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingCaseOwnerHistoryRepository extends JpaRepository<OnboardingCaseOwnerHistory, UUID> {

    List<OnboardingCaseOwnerHistory> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);
}
