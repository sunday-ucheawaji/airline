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
    /** Who performed the action this entry records: staff, or the applicant for INFORMATION_PROVIDED and WITHDRAWN. */
    private Long actorUserId;
    private String decision;
    private String comments;
    /** The user this entry is about: the nominated airline owner, or the assignee of a stage. */
    private Long targetUserId;
    /** The review stage this entry belongs to (COMPLIANCE, COMMERCIAL, TECHNICAL); null when it is about the whole review. */
    private String stage;
    private Instant createdAt;
}
