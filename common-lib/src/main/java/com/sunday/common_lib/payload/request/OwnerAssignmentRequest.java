package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerAssignmentRequest {

    @NotNull(message = ErrorMessageUtil.OWNER_USER_ID_MANDATORY)
    private Long ownerUserId;

    private String comments;
}
