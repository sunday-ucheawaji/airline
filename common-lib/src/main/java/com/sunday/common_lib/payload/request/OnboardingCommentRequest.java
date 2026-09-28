package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/** An internal staff note on an application; never shown to the applicant. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingCommentRequest {

    @NotBlank(message = ErrorMessageUtil.COMMENT_MESSAGE_MANDATORY)
    private String message;
}
