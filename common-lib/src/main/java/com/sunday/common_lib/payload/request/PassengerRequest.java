package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.Gender;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerRequest {

    /** Ignored on create. On update, identifies the passenger; required for a lap infant, who has no seat. */
    private UUID id;

    @NotBlank(message = ErrorMessageUtil.PASSENGER_FIRST_NAME_MANDATORY)
    @Size(min = 2, max = 50, message = ErrorMessageUtil.PASSENGER_FIRST_NAME_SIZE)
    private String firstName;

    @NotBlank(message = ErrorMessageUtil.PASSENGER_LAST_NAME_MANDATORY)
    @Size(min = 2, max = 50, message = ErrorMessageUtil.PASSENGER_LAST_NAME_SIZE)
    private String lastName;

    @NotBlank(message = ErrorMessageUtil.PASSENGER_EMAIL_MANDATORY)
    @Email(message = ErrorMessageUtil.EMAIL_INVALID)
    private String email;

    @NotBlank(message = ErrorMessageUtil.PASSENGER_PHONE_MANDATORY)
    @Pattern(regexp = "^\\+[0-9]{1,3}[0-9]{4,14}$", message = ErrorMessageUtil.PASSENGER_PHONE_FORMAT_INVALID)
    private String phone;

    @NotNull(message = ErrorMessageUtil.PASSENGER_DOB_MANDATORY)
    @Past(message = ErrorMessageUtil.PASSENGER_DOB_PAST)
    private LocalDate dateOfBirth;

    @NotNull(message = ErrorMessageUtil.PASSENGER_GENDER_MANDATORY)
    private Gender gender;

    private String seatNumber;

    /** Required except for an infant travelling on an adult's lap. */
    private UUID seatInstanceId;

    private String passportNumber;
    private String nationality;
    private String frequentFlyerNumber;

    private Boolean requiresWheelchairAssistance = false;
    private String dietaryPreferences;
    private String medicalConditions;
}
