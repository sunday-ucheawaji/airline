package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/** The applicant's text answer to an information request. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InformationResponseRequest {

    @NotBlank(message = ErrorMessageUtil.INFORMATION_RESPONSE_MANDATORY)
    private String response;
}
