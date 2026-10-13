package com.sunday.services.client;

import java.util.UUID;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.payload.response.AircraftResponse;
import com.sunday.common_lib.payload.response.AirlineResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Rethrows the original {@code Forbidden}/{@code NotFound} cause unchanged so the existing
 * type-specific catches in {@code AirlineIntegrationServiceImpl} still see them — only a genuine
 * outage degrades to the fail-open/fail-closed defaults below.
 */
@Slf4j
@Component
public class AirlineClientFallbackFactory implements FallbackFactory<AirlineClient> {

    @Override
    public AirlineClient create(Throwable cause) {
        return new AirlineClient() {
            @Override
            public List<AirlineResponse> getMyAirlines(UUID userId) {
                log.warn("airline-core-service call failed – membership lookup for user {} returns null: {}",
                        userId, cause.getMessage(), cause);
                return null;
            }

            @Override
            public List<String> getMyPermissions(UUID airlineId, UUID userId) {
                if (cause instanceof FeignException.Forbidden forbidden) {
                    throw forbidden;
                }
                log.warn("airline-core-service call failed – permission lookup for user {} on airline {} returns null: {}",
                        userId, airlineId, cause.getMessage(), cause);
                return null;
            }

            @Override
            public void requirePermission(List<UUID> airlineIds, String permission, UUID userId) {
                if (cause instanceof FeignException.Forbidden forbidden) {
                    throw forbidden;
                }
                log.warn("airline-core-service call failed – batch permission check for user {} on airlines {} could not be verified: {}",
                        userId, airlineIds, cause.getMessage(), cause);
                throw new ServiceUnavailableException(ErrorMessageUtil.AIRLINE_SERVICE_UNAVAILABLE, cause);
            }

            @Override
            public AircraftResponse getAircraftById(UUID id) {
                if (cause instanceof FeignException.Forbidden forbidden) {
                    throw forbidden;
                }
                if (cause instanceof FeignException.NotFound notFound) {
                    throw notFound;
                }
                log.warn("airline-core-service call failed – aircraft {} lookup returns null: {}", id, cause.getMessage(), cause);
                return null;
            }
        };
    }
}
