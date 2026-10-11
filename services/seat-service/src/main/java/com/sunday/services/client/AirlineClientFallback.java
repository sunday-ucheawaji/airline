package com.sunday.services.client;

import java.util.UUID;
import com.sunday.common_lib.payload.response.AircraftResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import org.springframework.stereotype.Component;

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
        throw new RuntimeException(ErrorMessageUtil.AIRLINE_SERVICE_UNAVAILABLE);
    }

    @Override
    public AircraftResponse getAircraftByIdInternal(UUID id) {
        return null;
    }

    @Override
    public Map<UUID, AircraftResponse> getAircraftsByIdsInternal(List<UUID> ids) {
        return null;
    }
}
