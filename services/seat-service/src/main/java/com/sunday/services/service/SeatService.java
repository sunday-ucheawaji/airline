package com.sunday.services.service;

import com.sunday.common_lib.payload.request.SeatRequest;
import com.sunday.common_lib.payload.response.SeatResponse;

import java.util.List;

public interface SeatService {


    void generateSeats(Long seatMapId) throws Exception;
    SeatResponse getSeatById(Long id);
    List<SeatResponse> getAll();
    SeatResponse updateSeat(Long id, SeatRequest request);

}
