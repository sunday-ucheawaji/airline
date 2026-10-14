package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.enums.PassengerType;
import com.sunday.common_lib.payload.response.FareQuoteResponse;

import java.time.LocalDate;

public interface FareQuoteService {

    /**
     * Prices the fare for one passenger type and departure date. An adult fare also prices children and infants,
     * through the fare filed alongside it for that type when there is one. Conflict (409) names the rule the priced
     * fare breaks for that date, or if the fare does not belong to {@code flightId}/{@code cabinClass}/{@code passengerType}.
     */
    FareQuoteResponse quote(UUID fareId, UUID flightId, CabinClassType cabinClass, PassengerType passengerType,
                            LocalDate departureDate);
}
