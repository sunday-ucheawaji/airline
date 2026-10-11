package com.sunday.services.clients;

import java.util.UUID;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.payload.response.FlightInstanceResponse;
import com.sunday.common_lib.payload.response.FlightResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Fails closed: a missing flight must not turn into a null that breaks later, or a booking without a flight. */
@Component
public class FlightClientFallback implements FlightClient {

    @Override
    public FlightResponse getFlightById(UUID id) {
        throw new ServiceUnavailableException(ErrorMessageUtil.FLIGHT_SERVICE_UNAVAILABLE);
    }

    @Override
    public FlightInstanceResponse getFlightInstanceResponse(UUID id) {
        throw new ServiceUnavailableException(ErrorMessageUtil.FLIGHT_SERVICE_UNAVAILABLE);
    }

    @Override
    public Map<UUID, FlightResponse> getFlightsByIds(List<UUID> ids) {
        throw new ServiceUnavailableException(ErrorMessageUtil.FLIGHT_SERVICE_UNAVAILABLE);
    }

    @Override
    public Map<UUID, FlightInstanceResponse> getFlightInstancesByIds(List<UUID> ids) {
        throw new ServiceUnavailableException(ErrorMessageUtil.FLIGHT_SERVICE_UNAVAILABLE);
    }
}
