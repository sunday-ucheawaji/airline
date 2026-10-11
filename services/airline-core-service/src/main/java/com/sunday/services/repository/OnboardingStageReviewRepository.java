package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.services.enums.OnboardingStage;
import com.sunday.services.model.OnboardingStageReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OnboardingStageReviewRepository extends JpaRepository<OnboardingStageReview, UUID> {

    List<OnboardingStageReview> findByApplicationId(UUID applicationId);

    Optional<OnboardingStageReview> findByApplicationIdAndStage(UUID applicationId, OnboardingStage stage);
}
