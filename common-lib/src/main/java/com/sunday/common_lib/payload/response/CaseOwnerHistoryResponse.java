package com.sunday.common_lib.payload.response;

import lombok.*;

import java.time.Instant;

/** One entry in the audit trail of who has owned an application's review. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseOwnerHistoryResponse {

    private Long id;
    private Long applicationId;
    private Long fromUserId;
    private Long toUserId;
    private Long actorUserId;
    /** CLAIMED, TRANSFERRED, RELEASED or TAKEN_OVER. */
    private String action;
    private String reason;
    private Instant createdAt;
}
