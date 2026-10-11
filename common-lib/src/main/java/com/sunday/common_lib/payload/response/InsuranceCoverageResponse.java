package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.enums.CoverageType;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceCoverageResponse {
    private UUID id;
    private UUID ancillaryId;
    private String ancillaryName;
    private CoverageType coverageType;
    private String name;
    private String description;
    private BigDecimal coverageAmount;
    private String currency;
    private Boolean isFlat;
    private String claimCondition;
    private String emergencyContact;
    private Integer displayOrder;
    private Boolean active;
}
