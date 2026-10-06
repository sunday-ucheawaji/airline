package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.response.FareQuoteResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.model.Fare;
import com.sunday.services.repository.FareRepository;
import com.sunday.services.service.FareQuoteService;
import com.sunday.services.validation.FareValidityPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FareQuoteServiceImpl implements FareQuoteService {

    private final FareRepository fareRepository;
    private final Clock clock;

    @Override
    public FareQuoteResponse quote(Long fareId, LocalDate departureDate) {
        Fare fare = fareRepository.findById(fareId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.FARE_NOT_FOUND_BY_ID, fareId)));
        Instant now = Instant.now(clock);
        FareValidityPolicy.violation(fare, departureDate, now).ifPresent(reason -> {
            throw new ConflictException(reason);
        });

        return FareQuoteResponse.builder()
                .fareId(fare.getId())
                .version(fare.getVersion())
                .departureDate(departureDate)
                .currency(fare.getCurrency())
                .baseFare(fare.getBaseFare().doubleValue())
                .taxesAndFees(fare.getTaxesAndFees() != null ? fare.getTaxesAndFees().doubleValue() : null)
                .airlineFees(fare.getAirlineFees() != null ? fare.getAirlineFees().doubleValue() : null)
                .totalPrice(fare.getTotalAmount().doubleValue())
                .fareRulesId(fare.getFareRules() != null ? fare.getFareRules().getId() : null)
                .baggagePolicyId(fare.getBaggagePolicy() != null ? fare.getBaggagePolicy().getId() : null)
                .build();
    }
}
