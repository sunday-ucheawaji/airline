package com.sunday.services.integration;

import com.sunday.common_lib.payload.response.FareQuoteResponse;

import java.time.LocalDate;

public interface PricingIntegrationService {

    /** Prices the fare for one departure. Throws ConflictException if the fare is not sellable on that date. */
    FareQuoteResponse quoteFare(Long fareId, LocalDate departureDate);
}
