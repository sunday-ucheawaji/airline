package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

/** Names the specialist who will decide one review stage. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageAssigneeRequest {

    @NotNull(message = ErrorMessageUtil.ASSIGNEE_USER_ID_MANDATORY)
    private UUID assigneeUserId;

    private String comments;
}
