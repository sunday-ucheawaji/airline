package com.sunday.services.client;

import java.util.UUID;
import com.sunday.common_lib.payload.response.AircraftResponse;
import com.sunday.common_lib.payload.response.AirlineResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class AirlineClientFallback implements AirlineClient {

    @Override
    public List<String> getMyPermissions(UUID airlineId, UUID userId) {
        return null;
    }

    @Override
    public void requirePermission(List<UUID> airlineIds, String permission, UUID userId) {
        // A void method can't signal "unavailable" via a null return — throw directly instead.
        throw new RuntimeException(ErrorMessageUtil.AIRLINE_SERVICE_UNAVAILABLE);
    }

    @Override
    public AirlineResponse getAirlineByIdInternal(UUID id) {
        return null;
    }

    @Override
    public Map<UUID, AirlineResponse> getAirlinesByIdsInternal(List<UUID> ids) {
        return Collections.emptyMap();
    }

    @Override
    public AircraftResponse getAircraftById(UUID id, UUID userId) {
        return null;
    }

    @Override
    public AircraftResponse getAircraftByIdInternal(UUID id) {
        return null;
    }

    @Override
    public Map<UUID, AircraftResponse> getAircraftsByIdsInternal(List<UUID> ids) {
        return Collections.emptyMap();
    }

    @Override
    public List<AirlineResponse> getAirlinesByIataCodes(List<String> codes) {
        return Collections.emptyList();
    }

    @Override
    public List<AirlineResponse> getAirlinesByAlliance(String alliance) {
        return Collections.emptyList();
    }
}
