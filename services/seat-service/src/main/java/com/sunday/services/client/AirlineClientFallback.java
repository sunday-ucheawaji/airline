package com.sunday.services.client;

import com.sunday.common_lib.payload.response.AircraftResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AirlineClientFallback implements AirlineClient {

    @Override
    public List<String> getMyPermissions(Long airlineId, Long userId) {
        return null;
    }

    @Override
    public void requirePermission(List<Long> airlineIds, String permission, Long userId) {
        throw new RuntimeException(ErrorMessageUtil.AIRLINE_SERVICE_UNAVAILABLE);
    }

    @Override
    public AircraftResponse getAircraftByIdInternal(Long id) {
        return null;
    }

    @Override
    public Map<Long, AircraftResponse> getAircraftsByIdsInternal(List<Long> ids) {
        return null;
    }
}
