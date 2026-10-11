package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.payload.request.SeatRequest;
import com.sunday.common_lib.payload.response.SeatResponse;
import com.sunday.services.model.SeatMap;

import java.util.Collection;

public interface SeatService {

    void generateSeats(Collection<SeatMap> seatMaps);
    SeatResponse getSeatById(UUID userId, UUID id);
    SeatResponse updateSeat(UUID userId, UUID id, SeatRequest request);
}
