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
public class VerifyEmailRequest {

    @NotBlank(message = ErrorMessageUtil.TOKEN_REQUIRED)
    @Size(max = 512, message = ErrorMessageUtil.INVALID_VERIFICATION_TOKEN)
    private String token;
}
