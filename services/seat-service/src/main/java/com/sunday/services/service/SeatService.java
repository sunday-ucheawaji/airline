package com.sunday.services.service;

import com.sunday.common_lib.payload.request.SeatRequest;
import com.sunday.common_lib.payload.response.SeatResponse;
import com.sunday.services.model.SeatMap;

import java.util.Collection;

public interface SeatService {

    void generateSeats(Collection<SeatMap> seatMaps);
    SeatResponse getSeatById(Long userId, Long id);
    SeatResponse updateSeat(Long userId, Long id, SeatRequest request);
}
