package com.sunday.common_lib.payload.response;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingReviewResponse {

    private Long id;
    private Long applicationId;
    private Long reviewerUserId;
    private String decision;
    private String comments;
    private Long assignedOwnerUserId;
    private Instant createdAt;
}
