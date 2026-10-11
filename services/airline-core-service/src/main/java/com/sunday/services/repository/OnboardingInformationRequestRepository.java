package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.services.enums.InformationRequestStatus;
import com.sunday.services.model.OnboardingInformationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OnboardingInformationRequestRepository extends JpaRepository<OnboardingInformationRequest, UUID> {

    List<OnboardingInformationRequest> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);

    List<OnboardingInformationRequest> findByApplicationIdAndStatus(UUID applicationId, InformationRequestStatus status);

    boolean existsByApplicationIdAndStatus(UUID applicationId, InformationRequestStatus status);

    Optional<OnboardingInformationRequest> findByIdAndApplicationId(UUID id, UUID applicationId);
}
