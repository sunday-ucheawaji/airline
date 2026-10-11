package com.sunday.services.client;

import java.util.UUID;
import com.sunday.common_lib.payload.response.CabinClassResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class SeatClientFallback implements SeatClient {

    @Override
    public List<CabinClassResponse> getCabinClassesByAircraftId(UUID aircraftId) {
        return Collections.emptyList();
    }
}
