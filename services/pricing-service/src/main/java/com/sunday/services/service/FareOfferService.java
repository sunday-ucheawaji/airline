package com.sunday.services.service;

import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.payload.response.FareOfferResponse;

import java.time.LocalDate;
import java.util.List;

public interface FareOfferService {

    /** Published fares on one flight and cabin that can be bought for the departure date, cheapest first. Public, read-only. */
    List<FareOfferResponse> offers(Long flightId, LocalDate departureDate, CabinClassType cabinClass);
}
