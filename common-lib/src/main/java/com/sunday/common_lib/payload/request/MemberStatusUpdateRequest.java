package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/** Suspends or reactivates an existing member (ACTIVE &lt;-&gt; SUSPENDED only — not for invites or removal). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberStatusUpdateRequest {

    @NotBlank(message = ErrorMessageUtil.MEMBER_STATUS_MANDATORY)
    private String status;
}
