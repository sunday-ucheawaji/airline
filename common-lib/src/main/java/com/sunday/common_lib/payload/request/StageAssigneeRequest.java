package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/** Names the specialist who will decide one review stage. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageAssigneeRequest {

    @NotNull(message = ErrorMessageUtil.ASSIGNEE_USER_ID_MANDATORY)
    private Long assigneeUserId;

    private String comments;
}
