package com.sunday.services.integration.impl;

import com.sunday.common_lib.payload.response.FareResponse;
import com.sunday.services.clients.PricingClient;
import com.sunday.services.integration.PricingIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PriceIntegrationServiceImpl implements PricingIntegrationService {

    private final PricingClient pricingClient;

    @Override
    public Double calculateFareTotal(Long fareId) {
        FareResponse fare=pricingClient.getFareById(fareId);
        Double baseFare = fare.getBaseFare();
        Double taxesAndFees = fare.getTaxesAndFees() != null ? fare.getTaxesAndFees() : 0.0;
        Double airlineFees = fare.getAirlineFees() != null ? fare.getAirlineFees() : 0.0;
        return baseFare+taxesAndFees+airlineFees;

    }
}
