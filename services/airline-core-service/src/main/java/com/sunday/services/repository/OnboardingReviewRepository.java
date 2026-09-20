package com.sunday.services.repository;

import com.sunday.services.model.OnboardingReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingReviewRepository extends JpaRepository<OnboardingReview, Long> {

    List<OnboardingReview> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);
}
