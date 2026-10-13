package com.sunday.services.service.impl;

import java.util.UUID;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.response.FareQuoteResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.model.Fare;
import com.sunday.services.model.FareRules;
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
    public FareQuoteResponse quote(UUID fareId, UUID flightId, CabinClassType cabinClass, LocalDate departureDate) {
        Fare fare = fareRepository.findById(fareId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.FARE_NOT_FOUND_BY_ID, fareId)));
        if (!fare.getFlightId().equals(flightId)) {
            throw new ConflictException(String.format(ErrorMessageUtil.FARE_FLIGHT_MISMATCH, fareId, flightId));
        }
        if (fare.getCabinClass() != cabinClass) {
            throw new ConflictException(String.format(ErrorMessageUtil.FARE_CABIN_CLASS_MISMATCH, fareId, cabinClass));
        }
        Instant now = Instant.now(clock);
        FareValidityPolicy.violation(fare, departureDate, now).ifPresent(reason -> {
            throw new ConflictException(reason);
        });

        FareRules rules = fare.getFareRules();
        return FareQuoteResponse.builder()
                .fareId(fare.getId())
                .version(fare.getVersion())
                .departureDate(departureDate)
                .currency(fare.getCurrency())
                .baseFare(fare.getBaseFare())
                .taxesAndFees(fare.getTaxesAndFees())
                .airlineFees(fare.getAirlineFees())
                .totalPrice(fare.getTotalAmount())
                .fareRulesId(rules != null ? rules.getId() : null)
                .baggagePolicyId(fare.getBaggagePolicy() != null ? fare.getBaggagePolicy().getId() : null)
                .refundable(rules != null && Boolean.TRUE.equals(rules.getIsRefundable()))
                .cancellationFee(rules != null ? rules.getCancellationFee() : null)
                .refundDeadlineDays(rules != null ? rules.getRefundDeadlineDays() : null)
                .build();
    }
}
