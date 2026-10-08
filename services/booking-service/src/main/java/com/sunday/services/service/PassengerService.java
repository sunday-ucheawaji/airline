package com.sunday.services.service;

import com.sunday.common_lib.payload.request.PassengerRequest;
import com.sunday.common_lib.payload.response.PassengerResponse;
import com.sunday.services.model.Passenger;

public interface PassengerService {

    PassengerResponse createPassenger(PassengerRequest request, Long userId);

    /** Always a new record for this booking. Records are never matched across users or bookings. */
    Passenger createPassengerEntity(PassengerRequest request, Long userId);

    boolean existsById(Long id);

    long count();
}
