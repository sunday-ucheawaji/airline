package com.sunday.services.client;

import java.util.UUID;
import com.sunday.common_lib.payload.request.CabinAvailabilityRequest;
import com.sunday.common_lib.payload.response.CabinClassResponse;
import com.sunday.common_lib.payload.response.CabinSeatStatusResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class SeatClientFallback implements SeatClient {

    @Override
    public List<CabinClassResponse> getCabinClassesByAircraftId(UUID aircraftId) {
        return Collections.emptyList();
    }

    @Override
    public List<CabinSeatStatusResponse> getSeatStatusSummary(UUID flightInstanceId) {
        return null;
    }

    @Override
    public Map<UUID, Long> countAvailableSeats(CabinAvailabilityRequest request) {
        return null;
    }

    @Override
    public Map<UUID, Long> countSoldSeats(List<UUID> flightInstanceIds) {
        return null;
    }
}
