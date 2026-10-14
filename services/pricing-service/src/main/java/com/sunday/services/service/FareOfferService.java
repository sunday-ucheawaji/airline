package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.enums.PassengerType;
import com.sunday.common_lib.payload.response.FareOfferResponse;

import java.time.LocalDate;
import java.util.List;

public interface FareOfferService {

    /** Published fares on one flight, cabin and passenger type that can be bought for the departure date, cheapest first. */
    List<FareOfferResponse> offers(UUID flightId, LocalDate departureDate, CabinClassType cabinClass, PassengerType passengerType);
}
