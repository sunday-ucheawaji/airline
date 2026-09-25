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
public class RoleRequest {

    @NotBlank
    private String name;

    private String description;

    /** "PLATFORM", "AIRLINE" or "BASELINE" — defaults to AIRLINE when omitted. */
    @Pattern(regexp = "PLATFORM|AIRLINE|BASELINE", message = ErrorMessageUtil.ROLE_SCOPE_INVALID)
    private String scope;
}
