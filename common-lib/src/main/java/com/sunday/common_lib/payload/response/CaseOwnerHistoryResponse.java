package com.sunday.common_lib.payload.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** One entry in the audit trail of who has owned an application's review. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseOwnerHistoryResponse {

    private UUID id;
    private UUID applicationId;
    private UUID fromUserId;
    private UUID toUserId;
    private UUID actorUserId;
    /** CLAIMED, TRANSFERRED, RELEASED or TAKEN_OVER. */
    private String action;
    private String reason;
    private Instant createdAt;
}
