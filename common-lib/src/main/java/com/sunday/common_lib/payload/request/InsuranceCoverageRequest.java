package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.CoverageType;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceCoverageRequest {

    @NotNull(message = "Ancillary ID is required")
    private Long ancillaryId;

    @NotNull(message = "Coverage type is required")
    private CoverageType coverageType;

    @NotBlank(message = "Coverage name is required")
    @Size(max = 200, message = "Coverage name cannot be longer than 200 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot be longer than 1000 characters")
    private String description;

    @NotNull(message = ErrorMessageUtil.COVERAGE_AMOUNT_MANDATORY)
    @PositiveOrZero(message = ErrorMessageUtil.COVERAGE_AMOUNT_MUST_BE_ZERO_OR_POSITIVE)
    private BigDecimal coverageAmount;

    private String currency;

    private Boolean isFlat;

    @Size(max = 500, message = "Claim condition cannot be longer than 500 characters")
    private String claimCondition;

    @Size(max = 100, message = "Emergency contact cannot be longer than 100 characters")
    private String emergencyContact;

    private Integer displayOrder;

    private Boolean active;
}
