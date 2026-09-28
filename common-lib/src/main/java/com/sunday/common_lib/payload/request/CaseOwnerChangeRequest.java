package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/** Hands a case to another onboarding officer (transfer), or forces the change (super-admin takeover, reason required). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseOwnerChangeRequest {

    @NotNull(message = ErrorMessageUtil.NEW_CASE_OWNER_MANDATORY)
    private Long newCaseOwnerUserId;

    private String reason;
}
