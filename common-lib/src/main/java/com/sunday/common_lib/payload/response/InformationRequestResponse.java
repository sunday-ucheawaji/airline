package com.sunday.common_lib.payload.response;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InformationRequestResponse {

    private Long id;
    private Long applicationId;
    /** The stage that asked (COMPLIANCE, COMMERCIAL, TECHNICAL); null when the case owner asked for the whole review. */
    private String stage;
    private Long requestedByUserId;
    private String message;
    /** OPEN, ANSWERED or CANCELLED. */
    private String status;
    private String response;
    private Instant createdAt;
    private Instant respondedAt;
}
