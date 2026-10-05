package com.sunday.services.service;

import com.sunday.common_lib.payload.request.SeatMapRequest;
import com.sunday.common_lib.payload.response.SeatMapBulkCreateResponse;
import com.sunday.common_lib.payload.response.SeatMapResponse;

import java.util.List;

public interface SeatMapService {

    SeatMapResponse createSeatMap(Long userId, SeatMapRequest request);
    SeatMapBulkCreateResponse createSeatMaps(Long userId, List<SeatMapRequest> requests);
    SeatMapResponse getSeatMapById(Long userId, Long id);
    SeatMapResponse getSeatMapsByCabinClass(Long userId, Long cabinClassId);
    SeatMapResponse updateSeatMap(Long userId, Long id, SeatMapRequest request);
    void deleteSeatMap(Long userId, Long id);
}
