package com.sunday.services.integration;

import java.util.UUID;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.enums.PassengerType;
import com.sunday.common_lib.payload.response.FareQuoteResponse;

import java.time.LocalDate;

public interface PricingIntegrationService {

    /**
     * Prices the fare for one passenger type and departure. Throws ConflictException if the fare is not sellable on
     * that date, or if it does not belong to {@code flightId}/{@code cabinClass} — a fare priced for a different
     * flight or cabin must never be attached to this booking. Children and infants on an adult fare are priced by
     * the fare pricing-service files alongside it for their type, or by the adult fare if there is none.
     */
    FareQuoteResponse quoteFare(UUID fareId, UUID flightId, CabinClassType cabinClass, PassengerType passengerType,
                                LocalDate departureDate);
}
