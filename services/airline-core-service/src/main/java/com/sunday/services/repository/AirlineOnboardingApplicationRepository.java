package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.services.enums.OnboardingStatus;
import com.sunday.services.model.AirlineOnboardingApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AirlineOnboardingApplicationRepository extends JpaRepository<AirlineOnboardingApplication, UUID> {

    List<AirlineOnboardingApplication> findByApplicantUserId(UUID applicantUserId);

    Page<AirlineOnboardingApplication> findByStatus(OnboardingStatus status, Pageable pageable);

    Page<AirlineOnboardingApplication> findByStatusAndCaseOwnerUserId(
            OnboardingStatus status, UUID caseOwnerUserId, Pageable pageable);
}
