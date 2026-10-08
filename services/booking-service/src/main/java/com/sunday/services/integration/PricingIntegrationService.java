package com.sunday.services.integration;

import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.payload.response.FareQuoteResponse;

import java.time.LocalDate;

public interface PricingIntegrationService {

    /**
     * Prices the fare for one departure. Throws ConflictException if the fare is not sellable on that date, or if
     * it does not belong to {@code flightId}/{@code cabinClass} — a fare priced for a different flight or cabin
     * must never be attached to this booking.
     */
    FareQuoteResponse quoteFare(Long fareId, Long flightId, CabinClassType cabinClass, LocalDate departureDate);
}
