package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/** A question staff put to the applicant while the application stays in review. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InformationRequestCreateRequest {

    @NotBlank(message = ErrorMessageUtil.COMMENT_MESSAGE_MANDATORY)
    private String message;
}
