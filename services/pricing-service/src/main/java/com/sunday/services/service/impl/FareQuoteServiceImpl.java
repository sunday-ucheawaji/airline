package com.sunday.services.service.impl;

import java.util.UUID;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.enums.PassengerType;
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
    public FareQuoteResponse quote(UUID fareId, UUID flightId, CabinClassType cabinClass, PassengerType passengerType,
                                   LocalDate departureDate) {
        Fare selected = fareRepository.findById(fareId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.FARE_NOT_FOUND_BY_ID, fareId)));
        if (!selected.getFlightId().equals(flightId)) {
            throw new ConflictException(String.format(ErrorMessageUtil.FARE_FLIGHT_MISMATCH, fareId, flightId));
        }
        if (selected.getCabinClass() != cabinClass) {
            throw new ConflictException(String.format(ErrorMessageUtil.FARE_CABIN_CLASS_MISMATCH, fareId, cabinClass));
        }
        Instant now = Instant.now(clock);
        Fare fare = fareFor(selected, passengerType, departureDate, now);
        FareValidityPolicy.violation(fare, departureDate, now).ifPresent(reason -> {
            throw new ConflictException(reason);
        });

        FareRules rules = fare.getFareRules();
        return FareQuoteResponse.builder()
                .fareId(fare.getId())
                .passengerType(fare.getPassengerType())
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
                .changeable(rules != null && Boolean.TRUE.equals(rules.getIsChangeable()))
                .changeFee(rules != null ? rules.getChangeFee() : null)
                .changeDeadlineHours(rules != null ? rules.getChangeDeadlineHours() : null)
                .build();
    }

    /**
     * The selected fare when it is for this passenger type. A child or infant on an adult fare is priced by the fare
     * filed alongside it for that type (same airline, flight, cabin, RBD and name) if that one is sellable, and
     * otherwise by the adult fare itself. Any other mismatch is refused.
     */
    private Fare fareFor(Fare selected, PassengerType passengerType, LocalDate departureDate, Instant now) {
        if (selected.getPassengerType() == passengerType) {
            return selected;
        }
        if (selected.getPassengerType() != PassengerType.ADT) {
            throw new ConflictException(String.format(
                    ErrorMessageUtil.FARE_PASSENGER_TYPE_MISMATCH, selected.getId(), passengerType));
        }
        return fareRepository.findByAirlineIdAndFlightIdAndCabinClassAndRbdCodeAndPassengerTypeAndName(
                        selected.getAirlineId(), selected.getFlightId(), selected.getCabinClass(), selected.getRbdCode(),
                        passengerType, selected.getName())
                .filter(sibling -> FareValidityPolicy.isSellable(sibling, departureDate, now))
                .orElse(selected);
    }
}
