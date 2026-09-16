package com.sunday.services.service;

import com.sunday.common_lib.payload.request.SeatMapRequest;
import com.sunday.common_lib.payload.response.SeatMapResponse;

import java.util.List;

public interface SeatMapService {

    SeatMapResponse createSeatMap(Long userId, SeatMapRequest request) throws Exception;
    List<SeatMapResponse> createSeatMaps(Long userId, List<SeatMapRequest> requests) throws Exception;
    SeatMapResponse getSeatMapById(Long id);

    SeatMapResponse getSeatMapsByCabinClass(Long cabinClassId);
    SeatMapResponse updateSeatMap(Long userId, Long id, SeatMapRequest request);
    void deleteSeatMap(Long id) throws Exception;
}
