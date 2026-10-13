package com.sunday.services.clients;

import java.util.UUID;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.payload.response.FlightInstanceResponse;
import com.sunday.common_lib.payload.response.FlightResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Fails closed: a missing flight must not turn into a null that breaks later, or a booking
 * without a flight. No call site around these single-id lookups distinguishes "not found" from
 * an outage today, so the translation happens here: a {@code NotFound} cause throws the
 * common-lib exception directly (404) instead of degrading to "unavailable" like a real failure.
 */
@Slf4j
@Component
public class FlightClientFallbackFactory implements FallbackFactory<FlightClient> {

    @Override
    public FlightClient create(Throwable cause) {
        return new FlightClient() {
            @Override
            public FlightResponse getFlightById(UUID id) {
                if (cause instanceof FeignException.NotFound) {
                    throw new ResourceNotFoundException(String.format(ErrorMessageUtil.FLIGHT_NOT_FOUND_BY_ID, id));
                }
                log.warn("flight-ops-service call failed – flight {} lookup could not be completed: {}", id, cause.getMessage(), cause);
                throw new ServiceUnavailableException(ErrorMessageUtil.FLIGHT_SERVICE_UNAVAILABLE, cause);
            }

            @Override
            public FlightInstanceResponse getFlightInstanceResponse(UUID id) {
                if (cause instanceof FeignException.NotFound) {
                    throw new ResourceNotFoundException(String.format(ErrorMessageUtil.FLIGHT_INSTANCE_NOT_FOUND_BY_ID, id));
                }
                log.warn("flight-ops-service call failed – flight instance {} lookup could not be completed: {}", id, cause.getMessage(), cause);
                throw new ServiceUnavailableException(ErrorMessageUtil.FLIGHT_SERVICE_UNAVAILABLE, cause);
            }

            @Override
            public Map<UUID, FlightResponse> getFlightsByIds(List<UUID> ids) {
                log.warn("flight-ops-service call failed – flight batch lookup could not be completed: {}", cause.getMessage(), cause);
                throw new ServiceUnavailableException(ErrorMessageUtil.FLIGHT_SERVICE_UNAVAILABLE, cause);
            }

            @Override
            public Map<UUID, FlightInstanceResponse> getFlightInstancesByIds(List<UUID> ids) {
                log.warn("flight-ops-service call failed – flight instance batch lookup could not be completed: {}", cause.getMessage(), cause);
                throw new ServiceUnavailableException(ErrorMessageUtil.FLIGHT_SERVICE_UNAVAILABLE, cause);
            }
        };
    }
}
