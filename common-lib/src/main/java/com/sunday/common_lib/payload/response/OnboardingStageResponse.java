package com.sunday.common_lib.payload.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingStageResponse {

    private UUID applicationId;
    /** COMPLIANCE, COMMERCIAL or TECHNICAL. */
    private String stage;
    /** PENDING, APPROVED or REJECTED. */
    private String status;
    private UUID assigneeUserId;
    private String comments;
    private Instant decidedAt;
}
