package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.payload.response.FareQuoteResponse;

import java.time.LocalDate;

public interface FareQuoteService {

    /**
     * Prices the fare for one departure date. Conflict (409) names the rule the fare breaks for that date, or if
     * the fare does not belong to {@code flightId}/{@code cabinClass}.
     */
    FareQuoteResponse quote(UUID fareId, UUID flightId, CabinClassType cabinClass, LocalDate departureDate);
}
