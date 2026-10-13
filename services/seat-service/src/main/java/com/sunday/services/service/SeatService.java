package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.payload.request.SeatRequest;
import com.sunday.common_lib.payload.response.SeatResponse;
import com.sunday.services.model.SeatMap;

import java.util.Collection;

public interface SeatService {

    void generateSeats(Collection<SeatMap> seatMaps);

    /** Replaces a seat map's seats after its layout changed; only allowed while no flight inventory uses them. */
    void regenerateSeats(SeatMap seatMap);
    SeatResponse getSeatById(UUID userId, UUID id);
    SeatResponse updateSeat(UUID userId, UUID id, SeatRequest request);
}
