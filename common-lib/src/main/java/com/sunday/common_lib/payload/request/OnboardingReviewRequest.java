package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.ReviewDecision;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingReviewRequest {

    @NotNull(message = ErrorMessageUtil.ONBOARDING_DECISION_MANDATORY)
    private ReviewDecision decision;

    private String comments;
}
