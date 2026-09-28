package com.sunday.services.repository;

import com.sunday.services.model.OnboardingCaseOwnerHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingCaseOwnerHistoryRepository extends JpaRepository<OnboardingCaseOwnerHistory, Long> {

    List<OnboardingCaseOwnerHistory> findByApplicationIdOrderByCreatedAtAsc(Long applicationId);
}
