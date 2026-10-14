package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.common_lib.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import static com.sunday.common_lib.validation.AccountRules.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {

    @NotBlank(message = ErrorMessageUtil.TOKEN_REQUIRED)
    @Size(max = 512, message = ErrorMessageUtil.INVALID_PASSWORD_RESET_TOKEN)
    private String token;

    @NotBlank(message = ErrorMessageUtil.NEW_PASSWORD_REQUIRED)
    @Size(min = PASSWORD_MIN, message = ErrorMessageUtil.PASSWORD_MIN_LENGTH)
    @MaxUtf8Bytes(value = PASSWORD_MAX_BYTES, message = ErrorMessageUtil.PASSWORD_TOO_LONG)
    private String newPassword;
}
