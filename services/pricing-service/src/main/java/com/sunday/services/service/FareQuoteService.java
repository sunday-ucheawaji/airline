package com.sunday.services.service;

import com.sunday.common_lib.payload.response.FareQuoteResponse;

import java.time.LocalDate;

public interface FareQuoteService {

    /** Prices the fare for one departure date. Conflict (409) names the rule the fare breaks for that date. */
    FareQuoteResponse quote(Long fareId, LocalDate departureDate);
}
