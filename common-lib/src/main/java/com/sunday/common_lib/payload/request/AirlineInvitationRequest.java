package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/** Invites an existing platform user, by email, to join an airline as ADMIN or VIEWER (never OWNER). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AirlineInvitationRequest {

    @NotBlank(message = ErrorMessageUtil.MEMBER_INVITE_EMAIL_MANDATORY)
    @Email(message = ErrorMessageUtil.MEMBER_INVITE_EMAIL_MANDATORY)
    private String email;

    @NotNull(message = ErrorMessageUtil.MEMBER_INVITE_ROLE_MANDATORY)
    private Long roleId;
}
