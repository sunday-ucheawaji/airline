package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.payload.request.SeatMapRequest;
import com.sunday.common_lib.payload.response.SeatMapBulkCreateResponse;
import com.sunday.common_lib.payload.response.SeatMapResponse;

import java.util.List;

public interface SeatMapService {

    SeatMapResponse createSeatMap(UUID userId, SeatMapRequest request);
    SeatMapBulkCreateResponse createSeatMaps(UUID userId, List<SeatMapRequest> requests);
    SeatMapResponse getSeatMapById(UUID userId, UUID id);
    SeatMapResponse getSeatMapsByCabinClass(UUID userId, UUID cabinClassId);
    SeatMapResponse updateSeatMap(UUID userId, UUID id, SeatMapRequest request);
    void deleteSeatMap(UUID userId, UUID id);
}
