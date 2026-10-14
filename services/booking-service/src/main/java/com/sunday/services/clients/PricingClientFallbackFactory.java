package com.sunday.services.clients;

import java.util.UUID;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.enums.PassengerType;
import com.sunday.common_lib.payload.response.FareQuoteResponse;
import com.sunday.common_lib.payload.response.FareResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Rethrows {@code Conflict}/{@code NotFound} unchanged so {@code PriceIntegrationServiceImpl}'s
 * own type-specific catches still see them — only a genuine outage degrades to null/empty.
 */
@Slf4j
@Component
public class PricingClientFallbackFactory implements FallbackFactory<PricingClient> {

    @Override
    public PricingClient create(Throwable cause) {
        return new PricingClient() {
            @Override
            public FareResponse getFareById(UUID id) {
                log.warn("pricing-service call failed – fare {} lookup returns null: {}", id, cause.getMessage(), cause);
                return null;
            }

            @Override
            public Map<UUID, FareResponse> getFaresByIds(List<UUID> ids) {
                log.warn("pricing-service call failed – fare batch lookup returns empty: {}", cause.getMessage(), cause);
                return Collections.emptyMap();
            }

            @Override
            public FareQuoteResponse quoteFare(UUID id, UUID flightId, CabinClassType cabinClass, PassengerType passengerType,
                                               LocalDate departureDate) {
                if (cause instanceof FeignException.Conflict conflict) {
                    throw conflict;
                }
                if (cause instanceof FeignException.NotFound notFound) {
                    throw notFound;
                }
                log.warn("pricing-service call failed – quote for fare {} returns null: {}", id, cause.getMessage(), cause);
                return null;
            }
        };
    }
}
