package com.sunday.common_lib.payload.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingApplicationResponse {

    private UUID id;

    private UUID applicantUserId;
    private UUID initialAdminUserId;

    private String legalName;
    private String displayName;

    private String iataCode;
    private String icaoCode;

    private String country;
    private String registrationNumber;

    private String alliance;
    private String website;
    private String logoUrl;

    private String supportEmail;
    private String supportPhone;
    private String supportHours;

    private UUID headquartersCityId;

    private String status;
    private String rejectionReason;
    private UUID airlineId;
    private UUID caseOwnerUserId;

    private Instant submittedAt;
    private Instant reviewedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
