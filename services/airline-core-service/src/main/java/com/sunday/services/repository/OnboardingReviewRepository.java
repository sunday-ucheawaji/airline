package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.services.model.OnboardingReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingReviewRepository extends JpaRepository<OnboardingReview, UUID> {

    List<OnboardingReview> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId);
}
