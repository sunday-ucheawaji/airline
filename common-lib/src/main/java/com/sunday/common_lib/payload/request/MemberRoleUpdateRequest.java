package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/** Changes an existing (ACTIVE) member's role between ADMIN and VIEWER — never OWNER, a separate concern. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberRoleUpdateRequest {

    @NotNull(message = ErrorMessageUtil.MEMBER_INVITE_ROLE_MANDATORY)
    private Long roleId;
}
