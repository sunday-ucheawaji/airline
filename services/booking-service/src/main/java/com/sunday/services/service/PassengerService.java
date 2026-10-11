package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.payload.request.PassengerRequest;
import com.sunday.common_lib.payload.response.PassengerResponse;
import com.sunday.services.model.Passenger;

public interface PassengerService {

    PassengerResponse createPassenger(PassengerRequest request, UUID userId);

    /** Always a new record for this booking. Records are never matched across users or bookings. */
    Passenger createPassengerEntity(PassengerRequest request, UUID userId);

    boolean existsById(UUID id);

    long count();
}
