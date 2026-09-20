package com.sunday.services.repository;

import com.sunday.services.enums.OnboardingStatus;
import com.sunday.services.model.AirlineOnboardingApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AirlineOnboardingApplicationRepository extends JpaRepository<AirlineOnboardingApplication, Long> {

    List<AirlineOnboardingApplication> findByApplicantUserId(Long applicantUserId);

    Page<AirlineOnboardingApplication> findByStatus(OnboardingStatus status, Pageable pageable);
}
