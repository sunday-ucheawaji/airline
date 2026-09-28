package com.sunday.common_lib.payload.response;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingStageResponse {

    private Long applicationId;
    /** COMPLIANCE, COMMERCIAL or TECHNICAL. */
    private String stage;
    /** PENDING, APPROVED or REJECTED. */
    private String status;
    private Long assigneeUserId;
    private String comments;
    private Instant decidedAt;
}
