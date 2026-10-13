package com.sunday.common_lib.payload.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AirlineIdentityChangeResponse {

    private UUID id;
    private UUID airlineId;
    private UUID requestedByUserId;
    private String currentLegalName;
    private String currentRegistrationNumber;
    private String currentIataCode;
    private String currentIcaoCode;
    private String proposedLegalName;
    private String proposedRegistrationNumber;
    private String proposedIataCode;
    private String proposedIcaoCode;
    private String reason;
    private String status;
    private UUID reviewedByUserId;
    private String reviewComment;
    private Instant reviewedAt;
    private Instant createdAt;
}
