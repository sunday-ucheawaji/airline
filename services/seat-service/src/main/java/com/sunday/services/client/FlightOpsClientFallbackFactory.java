package com.sunday.services.client;

import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.payload.response.FlightInstanceResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Fails closed: inventory is never attached to a flight instance that could not be looked up. */
@Slf4j
@Component
public class FlightOpsClientFallbackFactory implements FallbackFactory<FlightOpsClient> {

    @Override
    public FlightOpsClient create(Throwable cause) {
        return new FlightOpsClient() {
            @Override
            public FlightInstanceResponse getFlightInstance(UUID id) {
                if (cause instanceof FeignException.NotFound) {
                    throw new ResourceNotFoundException(String.format(ErrorMessageUtil.FLIGHT_INSTANCE_NOT_FOUND_BY_ID_FOR_SEATS, id));
                }
                log.warn("flight-ops-service call failed – flight instance {} lookup: {}", id, cause.getMessage(), cause);
                throw new ServiceUnavailableException(ErrorMessageUtil.FLIGHT_OPS_SERVICE_UNAVAILABLE, cause);
            }
        };
    }
}
