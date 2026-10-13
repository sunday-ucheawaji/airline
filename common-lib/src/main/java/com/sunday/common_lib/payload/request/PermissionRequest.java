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
public class PermissionRequest {

    @NotBlank
    private String name;

    private String description;

    /** "PLATFORM" or "AIRLINE"; defaults to PLATFORM when omitted. */
    @Pattern(regexp = "PLATFORM|AIRLINE", message = ErrorMessageUtil.PERMISSION_SCOPE_INVALID)
    private String scope;
}
