package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.common_lib.validation.OnCreate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Used both to create a draft and to update one (PATCH). Fields stay optional
 * for PATCH so omitting one leaves the existing value untouched; the four
 * mandatory fields are only enforced on creation, via the {@link OnCreate}
 * validation group. {@code requireCompleteForSubmission} in the service layer
 * remains the backstop against a later PATCH blanking one of them out.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingApplicationRequest {

    private Long initialAdminUserId;

    @NotBlank(message = ErrorMessageUtil.ONBOARDING_LEGAL_NAME_MANDATORY, groups = OnCreate.class)
    private String legalName;
    @NotBlank(message = ErrorMessageUtil.ONBOARDING_DISPLAY_NAME_MANDATORY, groups = OnCreate.class)
    private String displayName;

    @Size(min = 2, max = 2, message = "IATA code must be exactly 2 characters")
    private String iataCode;
    @Size(min = 3, max = 3, message = "ICAO code must be exactly 3 characters")
    private String icaoCode;

    @NotBlank(message = ErrorMessageUtil.ONBOARDING_COUNTRY_MANDATORY, groups = OnCreate.class)
    private String country;
    @NotBlank(message = ErrorMessageUtil.ONBOARDING_REGISTRATION_NUMBER_MANDATORY, groups = OnCreate.class)
    private String registrationNumber;

    private String alliance;
    private String website;
    private String logoUrl;

    private String supportEmail;
    private String supportPhone;
    private String supportHours;

    private Long headquartersCityId;
}
