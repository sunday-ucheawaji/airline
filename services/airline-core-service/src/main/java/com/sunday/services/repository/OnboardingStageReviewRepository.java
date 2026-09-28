package com.sunday.services.repository;

import com.sunday.services.enums.OnboardingStage;
import com.sunday.services.model.OnboardingStageReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OnboardingStageReviewRepository extends JpaRepository<OnboardingStageReview, Long> {

    List<OnboardingStageReview> findByApplicationId(Long applicationId);

    Optional<OnboardingStageReview> findByApplicationIdAndStage(Long applicationId, OnboardingStage stage);
}
