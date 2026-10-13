package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatusUpdateRequest {

    @NotBlank
    @Pattern(regexp = "ACTIVE|SUSPENDED", message = ErrorMessageUtil.USER_STATUS_INVALID)
    private String status;
}
