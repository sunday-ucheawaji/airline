package com.sunday.services.repository;

import com.sunday.services.enums.InformationRequestStatus;
import com.sunday.services.model.OnboardingInformationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OnboardingInformationRequestRepository extends JpaRepository<OnboardingInformationRequest, Long> {

    List<OnboardingInformationRequest> findByApplicationIdOrderByCreatedAtAsc(Long applicationId);

    List<OnboardingInformationRequest> findByApplicationIdAndStatus(Long applicationId, InformationRequestStatus status);

    boolean existsByApplicationIdAndStatus(Long applicationId, InformationRequestStatus status);

    Optional<OnboardingInformationRequest> findByIdAndApplicationId(Long id, Long applicationId);
}
