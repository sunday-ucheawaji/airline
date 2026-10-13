package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {

    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 8, message = ErrorMessageUtil.PASSWORD_MIN_LENGTH)
    @Size(max = 72, message = ErrorMessageUtil.PASSWORD_TOO_LONG)
    private String newPassword;
}
