package com.sunday.services.service.impl;

import java.util.UUID;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.enums.FareStatus;
import com.sunday.common_lib.payload.response.FareOfferResponse;
import com.sunday.services.model.Fare;
import com.sunday.services.repository.FareRepository;
import com.sunday.services.service.FareOfferService;
import com.sunday.services.validation.FareValidityPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FareOfferServiceImpl implements FareOfferService {

    private final FareRepository fareRepository;
    private final Clock clock;

    @Override
    public List<FareOfferResponse> offers(UUID flightId, LocalDate departureDate, CabinClassType cabinClass) {
        Instant now = Instant.now(clock);
        return fareRepository
                .findSearchCandidates(List.of(flightId), cabinClass, FareStatus.PUBLISHED, now, departureDate, departureDate)
                .stream()
                .filter(fare -> FareValidityPolicy.isSellable(fare, departureDate, now))
                .sorted(Comparator.comparing(Fare::getTotalAmount))
                .map(FareOfferServiceImpl::toOffer)
                .toList();
    }

    private static FareOfferResponse toOffer(Fare fare) {
        var seat = fare.getSeatBenefits();
        var boarding = fare.getBoardingBenefits();
        var inFlight = fare.getInFlightBenefits();
        var premium = fare.getPremiumServiceBenefits();
        var rules = fare.getFareRules();
        var baggage = fare.getBaggagePolicy();

        return FareOfferResponse.builder()
                .id(fare.getId())
                .name(fare.getName())
                .rbdCode(fare.getRbdCode())
                .fareLabel(fare.getFareLabel())
                .cabinClass(fare.getCabinClass())
                .passengerType(fare.getPassengerType())
                .travelFrom(fare.getTravelFrom())
                .travelTo(fare.getTravelTo())
                .currency(fare.getCurrency())
                .baseFare(fare.getBaseFare())
                .taxesAndFees(fare.getTaxesAndFees())
                .airlineFees(fare.getAirlineFees())
                .totalPrice(fare.getTotalAmount())
                .benefits(FareOfferResponse.Benefits.builder()
                        .extraSeatSpace(seat.getExtraSeatSpace())
                        .preferredSeatChoice(seat.getPreferredSeatChoice())
                        .advanceSeatSelection(seat.getAdvanceSeatSelection())
                        .guaranteedSeatTogether(seat.getGuaranteedSeatTogether())
                        .priorityBoarding(boarding.getPriorityBoarding())
                        .priorityCheckin(boarding.getPriorityCheckin())
                        .fastTrackSecurity(boarding.getFastTrackSecurity())
                        .complimentaryMeals(inFlight.getComplimentaryMeals())
                        .premiumMealChoice(inFlight.getPremiumMealChoice())
                        .inFlightInternet(inFlight.getInFlightInternet())
                        .inFlightEntertainment(inFlight.getInFlightEntertainment())
                        .complimentaryBeverages(inFlight.getComplimentaryBeverages())
                        .loungeAccess(premium.getLoungeAccess())
                        .airportTransfer(premium.getAirportTransfer())
                        .build())
                .rules(rules == null ? null : FareOfferResponse.Rules.builder()
                        .ruleName(rules.getRuleName())
                        .isRefundable(rules.getIsRefundable())
                        .cancellationFee(rules.getCancellationFee())
                        .refundDeadlineDays(rules.getRefundDeadlineDays())
                        .isChangeable(rules.getIsChangeable())
                        .changeFee(rules.getChangeFee())
                        .changeDeadlineHours(rules.getChangeDeadlineHours())
                        .build())
                .baggage(baggage == null ? null : FareOfferResponse.Baggage.builder()
                        .name(baggage.getName())
                        .description(baggage.getDescription())
                        .cabinBaggageMaxWeight(baggage.getCabinBaggageMaxWeight())
                        .cabinBaggagePieces(baggage.getCabinBaggagePieces())
                        .checkInBaggageMaxWeight(baggage.getCheckInBaggageMaxWeight())
                        .checkInBaggagePieces(baggage.getCheckInBaggagePieces())
                        .freeCheckedBagsAllowance(baggage.getFreeCheckedBagsAllowance())
                        .priorityBaggage(baggage.getPriorityBaggage())
                        .extraBaggageAllowance(baggage.getExtraBaggageAllowance())
                        .build())
                .build();
    }
}
