package com.sunday.services.integration.impl;

import java.util.UUID;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.payload.response.FareQuoteResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.clients.PricingClient;
import com.sunday.services.integration.PricingIntegrationService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PriceIntegrationServiceImpl implements PricingIntegrationService {

    private final PricingClient pricingClient;

    @Override
    public FareQuoteResponse quoteFare(UUID fareId, UUID flightId, CabinClassType cabinClass, LocalDate departureDate) {
        FareQuoteResponse quote;
        try {
            quote = pricingClient.quoteFare(fareId, flightId, cabinClass, departureDate);
        } catch (FeignException.Conflict e) {
            throw new ConflictException(ErrorMessageUtil.FARE_UNAVAILABLE_FOR_DEPARTURE);
        } catch (FeignException e) {
            throw new ServiceUnavailableException(String.format(ErrorMessageUtil.FARE_SERVICE_ERROR, e.getMessage()), e);
        }
        if (quote == null) {
            throw new ServiceUnavailableException(ErrorMessageUtil.FARE_SERVICE_UNAVAILABLE);
        }
        return quote;
    }
}
