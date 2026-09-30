package com.sunday.services.client;

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
    public List<String> getMyPermissions(Long airlineId, Long userId) {
        return null;
    }

    @Override
    public void requirePermission(List<Long> airlineIds, String permission, Long userId) {
        // A void method can't signal "unavailable" via a null return — throw directly instead.
        throw new RuntimeException(ErrorMessageUtil.AIRLINE_SERVICE_UNAVAILABLE);
    }

    @Override
    public AirlineResponse getAirlineByIdInternal(Long id) {
        return null;
    }

    @Override
    public Map<Long, AirlineResponse> getAirlinesByIdsInternal(List<Long> ids) {
        return Collections.emptyMap();
    }

    @Override
    public AircraftResponse getAircraftById(Long id, Long userId) {
        return null;
    }

    @Override
    public AircraftResponse getAircraftByIdInternal(Long id) {
        return null;
    }

    @Override
    public Map<Long, AircraftResponse> getAircraftsByIdsInternal(List<Long> ids) {
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
