package com.sunday.common_lib.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareSearchResult {

    private UUID flightId;
    private LocalDate departureDate;
    private FareResponse fare;
    private PartyPriceResponse partyPrice;

    public FareSearchResult(UUID flightId, LocalDate departureDate, FareResponse fare) {
        this(flightId, departureDate, fare, null);
    }
}
