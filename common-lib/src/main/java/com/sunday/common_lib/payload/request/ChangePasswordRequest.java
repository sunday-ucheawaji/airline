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
public class ChangePasswordRequest {

    @NotBlank(message = ErrorMessageUtil.CURRENT_PASSWORD_REQUIRED)
    @Size(max = 1024, message = ErrorMessageUtil.CURRENT_PASSWORD_INCORRECT)
    private String currentPassword;

    @NotBlank(message = ErrorMessageUtil.NEW_PASSWORD_REQUIRED)
    @Size(min = PASSWORD_MIN, message = ErrorMessageUtil.PASSWORD_MIN_LENGTH)
    @MaxUtf8Bytes(value = PASSWORD_MAX_BYTES, message = ErrorMessageUtil.PASSWORD_TOO_LONG)
    private String newPassword;
}
