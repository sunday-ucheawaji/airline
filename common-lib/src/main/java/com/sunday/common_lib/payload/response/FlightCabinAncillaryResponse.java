package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.enums.CabinClassType;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightCabinAncillaryResponse {
    private UUID id;
    private UUID flightId;
    private CabinClassType cabinClass;
    private AncillaryResponse ancillary;
    private Boolean available;
    private Integer maxQuantity;
    private BigDecimal price;
    private String currency;
    private Boolean includedInFare;
}
