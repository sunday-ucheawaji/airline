package com.sunday.common_lib.payload.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InformationRequestResponse {

    private UUID id;
    private UUID applicationId;
    /** The stage that asked (COMPLIANCE, COMMERCIAL, TECHNICAL); null when the case owner asked for the whole review. */
    private String stage;
    private UUID requestedByUserId;
    private String message;
    /** OPEN, ANSWERED or CANCELLED. */
    private String status;
    private String response;
    private Instant createdAt;
    private Instant respondedAt;
}
