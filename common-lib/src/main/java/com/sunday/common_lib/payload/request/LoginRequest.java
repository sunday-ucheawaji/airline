package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import static com.sunday.common_lib.validation.AccountRules.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = ErrorMessageUtil.EMAIL_REQUIRED)
    @Email(message = ErrorMessageUtil.EMAIL_FORMAT)
    @Size(max = EMAIL_MAX, message = ErrorMessageUtil.EMAIL_TOO_LONG)
    private String email;

    /** Only presence is checked: a wrong or over-long password is simply invalid credentials. */
    @NotBlank(message = ErrorMessageUtil.PASSWORD_REQUIRED)
    @Size(max = 1024, message = ErrorMessageUtil.INVALID_CREDENTIALS)
    private String password;
}
