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
public class ResendVerificationRequest {

    @NotBlank(message = ErrorMessageUtil.EMAIL_REQUIRED)
    @Email(message = ErrorMessageUtil.EMAIL_FORMAT)
    @Size(max = EMAIL_MAX, message = ErrorMessageUtil.EMAIL_TOO_LONG)
    private String email;
}
