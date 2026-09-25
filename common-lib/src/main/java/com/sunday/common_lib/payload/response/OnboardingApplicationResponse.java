package com.sunday.common_lib.payload.response;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingApplicationResponse {

    private Long id;

    private Long applicantUserId;
    private Long initialAdminUserId;

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

    private Long headquartersCityId;

    private String status;
    private String rejectionReason;
    private Long airlineId;

    private Instant submittedAt;
    private Instant reviewedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
