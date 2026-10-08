package com.sunday.services.clients;

import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.payload.response.FareQuoteResponse;
import com.sunday.common_lib.payload.response.FareResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class PricingClientFallback implements PricingClient {

    @Override
    public FareResponse getFareById(Long id) {
        return null;
    }

    @Override
    public Map<Long, FareResponse> getFaresByIds(List<Long> ids) {
        return Collections.emptyMap();
    }

    @Override
    public FareQuoteResponse quoteFare(Long id, Long flightId, CabinClassType cabinClass, LocalDate departureDate) {
        return null;
    }
}
