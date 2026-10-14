package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.common_lib.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import static com.sunday.common_lib.validation.AccountRules.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = ErrorMessageUtil.EMAIL_REQUIRED)
    @Email(message = ErrorMessageUtil.EMAIL_FORMAT)
    @Size(max = EMAIL_MAX, message = ErrorMessageUtil.EMAIL_TOO_LONG)
    private String email;

    @NotBlank(message = ErrorMessageUtil.PASSWORD_REQUIRED)
    @Size(min = PASSWORD_MIN, message = ErrorMessageUtil.PASSWORD_MIN_LENGTH)
    @MaxUtf8Bytes(value = PASSWORD_MAX_BYTES, message = ErrorMessageUtil.PASSWORD_TOO_LONG)
    private String password;

    @NotBlank(message = ErrorMessageUtil.FIRST_NAME_REQUIRED)
    @Size(max = NAME_MAX, message = ErrorMessageUtil.NAME_TOO_LONG)
    @Pattern(regexp = NAME_PATTERN, message = ErrorMessageUtil.NAME_INVALID_CHARACTERS)
    private String firstName;

    @NotBlank(message = ErrorMessageUtil.LAST_NAME_REQUIRED)
    @Size(max = NAME_MAX, message = ErrorMessageUtil.NAME_TOO_LONG)
    @Pattern(regexp = NAME_PATTERN, message = ErrorMessageUtil.NAME_INVALID_CHARACTERS)
    private String lastName;

    @Size(max = NAME_MAX, message = ErrorMessageUtil.NAME_TOO_LONG)
    @Pattern(regexp = NAME_PATTERN, message = ErrorMessageUtil.NAME_INVALID_CHARACTERS)
    private String middleName;

    @Pattern(regexp = PHONE_PATTERN, message = ErrorMessageUtil.PHONE_FORMAT)
    private String phoneNumber;
}
