package com.sunday.services.mapper;

import com.sunday.common_lib.embeddable.*;
import com.sunday.common_lib.enums.FareStatus;
import com.sunday.common_lib.enums.PassengerType;
import com.sunday.common_lib.payload.request.FareRequest;
import com.sunday.common_lib.payload.response.FareResponse;
import com.sunday.services.model.Fare;

public class FareMapper {

    public static Fare toEntity(FareRequest request, Long airlineId, Long userId) {
        if (request == null) return null;

        SeatBenefits seatBenefits = SeatBenefits.builder()
                .extraSeatSpace(bool(request.getExtraSeatSpace()))
                .preferredSeatChoice(bool(request.getPreferredSeatChoice()))
                .advanceSeatSelection(bool(request.getAdvanceSeatSelection()))
                .guaranteedSeatTogether(bool(request.getGuaranteedSeatTogether()))
                .build();

        BoardingBenefits boardingBenefits = BoardingBenefits.builder()
                .priorityBoarding(bool(request.getPriorityBoarding()))
                .priorityCheckin(bool(request.getPriorityCheckin()))
                .fastTrackSecurity(bool(request.getFastTrackSecurity()))
                .build();

        InFlightBenefits inFlightBenefits = InFlightBenefits.builder()
                .complimentaryMeals(bool(request.getComplimentaryMeals()))
                .premiumMealChoice(bool(request.getPremiumMealChoice()))
                .inFlightInternet(bool(request.getInFlightInternet()))
                .inFlightEntertainment(bool(request.getInFlightEntertainment()))
                .complimentaryBeverages(bool(request.getComplimentaryBeverages()))
                .build();

        FlexibilityBenefits flexibilityBenefits = FlexibilityBenefits.builder()
                .freeDateChange(bool(request.getFreeDateChange()))
                .partialRefund(bool(request.getPartialRefund()))
                .fullRefund(bool(request.getFullRefund()))
                .build();

        PremiumServiceBenefits premiumServiceBenefits = PremiumServiceBenefits.builder()
                .loungeAccess(bool(request.getLoungeAccess()))
                .airportTransfer(bool(request.getAirportTransfer()))
                .build();

        Fare fare = Fare.builder()
                .name(request.getName())
                .rbdCode(request.getRbdCode())
                .flightId(request.getFlightId())
                .cabinClassId(request.getCabinClassId())
                .cabinClass(request.getCabinClass())
                .passengerType(passengerTypeOf(request))
                .fareBasisCode(request.getFareBasisCode())
                .airlineId(airlineId)
                .status(FareStatus.DRAFT)
                .createdBy(userId)
                .updatedBy(userId)

                .currency(request.getCurrency())
                .baseFare(request.getBaseFare())
                .taxesAndFees(request.getTaxesAndFees())
                .airlineFees(request.getAirlineFees())
                .fareLabel(request.getFareLabel())
                .salesFrom(request.getSalesFrom())
                .salesTo(request.getSalesTo())
                .travelFrom(request.getTravelFrom())
                .travelTo(request.getTravelTo())
                .advancePurchaseDays(request.getAdvancePurchaseDays())
                .seatBenefits(seatBenefits)
                .boardingBenefits(boardingBenefits)
                .inFlightBenefits(inFlightBenefits)
                .flexibilityBenefits(flexibilityBenefits)
                .premiumServiceBenefits(premiumServiceBenefits)
                .build();
        fare.recalculateTotal();
        return fare;
    }

    public static FareResponse toResponse(Fare fare) {
        if (fare == null) return null;
        return FareResponse.builder()
                .id(fare.getId())
                .airlineId(fare.getAirlineId())
                .status(fare.getStatus())
                .name(fare.getName())
                .rbdCode(fare.getRbdCode())
                .flightId(fare.getFlightId())
                .cabinClassId(fare.getCabinClassId())
                .cabinClass(fare.getCabinClass())
                .passengerType(fare.getPassengerType())
                .fareBasisCode(fare.getFareBasisCode())
                .currency(fare.getCurrency())
                .baseFare(fare.getBaseFare())
                .taxesAndFees(fare.getTaxesAndFees())
                .airlineFees(fare.getAirlineFees())
                .totalPrice(fare.getTotalAmount())
                .fareLabel(fare.getFareLabel())
                .salesFrom(fare.getSalesFrom())
                .salesTo(fare.getSalesTo())
                .travelFrom(fare.getTravelFrom())
                .travelTo(fare.getTravelTo())
                .advancePurchaseDays(fare.getAdvancePurchaseDays())
                .fareRulesId(fare.getFareRules() != null ? fare.getFareRules().getId() : null)
                // Seat benefits
                .extraSeatSpace(fare.getSeatBenefits() != null ? fare.getSeatBenefits().getExtraSeatSpace() : false)
                .preferredSeatChoice(fare.getSeatBenefits() != null ? fare.getSeatBenefits().getPreferredSeatChoice() : false)
                .advanceSeatSelection(fare.getSeatBenefits() != null ? fare.getSeatBenefits().getAdvanceSeatSelection() : false)
                .guaranteedSeatTogether(fare.getSeatBenefits() != null ? fare.getSeatBenefits().getGuaranteedSeatTogether() : false)
                // Boarding benefits
                .priorityBoarding(fare.getBoardingBenefits() != null ? fare.getBoardingBenefits().getPriorityBoarding() : false)
                .priorityCheckin(fare.getBoardingBenefits() != null ? fare.getBoardingBenefits().getPriorityCheckin() : false)
                .fastTrackSecurity(fare.getBoardingBenefits() != null ? fare.getBoardingBenefits().getFastTrackSecurity() : false)
                // In-flight benefits
                .complimentaryMeals(fare.getInFlightBenefits() != null ? fare.getInFlightBenefits().getComplimentaryMeals() : false)
                .premiumMealChoice(fare.getInFlightBenefits() != null ? fare.getInFlightBenefits().getPremiumMealChoice() : false)
                .inFlightInternet(fare.getInFlightBenefits() != null ? fare.getInFlightBenefits().getInFlightInternet() : false)
                .inFlightEntertainment(fare.getInFlightBenefits() != null ? fare.getInFlightBenefits().getInFlightEntertainment() : false)
                .complimentaryBeverages(fare.getInFlightBenefits() != null ? fare.getInFlightBenefits().getComplimentaryBeverages() : false)
                // Flexibility benefits
                .freeDateChange(fare.getFlexibilityBenefits() != null ? fare.getFlexibilityBenefits().getFreeDateChange() : false)
                .partialRefund(fare.getFlexibilityBenefits() != null ? fare.getFlexibilityBenefits().getPartialRefund() : false)
                .fullRefund(fare.getFlexibilityBenefits() != null ? fare.getFlexibilityBenefits().getFullRefund() : false)
                // Premium service benefits
                .loungeAccess(fare.getPremiumServiceBenefits() != null ? fare.getPremiumServiceBenefits().getLoungeAccess() : false)
                .airportTransfer(fare.getPremiumServiceBenefits() != null ? fare.getPremiumServiceBenefits().getAirportTransfer() : false)
                // Nested responses
                .fareRules(fare.getFareRules() != null ? FareRulesMapper.toResponse(fare.getFareRules()) : null)
                .baggagePolicy(fare.getBaggagePolicy() != null ? BaggagePolicyMapper.toResponse(fare.getBaggagePolicy()) : null)
                .createdAt(fare.getCreatedAt())
                .updatedAt(fare.getUpdatedAt())
                .build();
    }

    /** flightId and airlineId are not updatable: a fare stays on the flight and airline it was created for. */
    public static void updateEntity(FareRequest request, Fare existing, Long userId) {
        if (request == null || existing == null) return;
        existing.setName(request.getName());
        existing.setRbdCode(request.getRbdCode());
        existing.setCabinClassId(request.getCabinClassId());
        existing.setCabinClass(request.getCabinClass());
        existing.setPassengerType(passengerTypeOf(request));
        existing.setFareBasisCode(request.getFareBasisCode());
        existing.setUpdatedBy(userId);

        existing.setCurrency(request.getCurrency());
        existing.setBaseFare(request.getBaseFare());
        existing.setTaxesAndFees(request.getTaxesAndFees());
        existing.setAirlineFees(request.getAirlineFees());
        existing.setFareLabel(request.getFareLabel());
        existing.setSalesFrom(request.getSalesFrom());
        existing.setSalesTo(request.getSalesTo());
        existing.setTravelFrom(request.getTravelFrom());
        existing.setTravelTo(request.getTravelTo());
        existing.setAdvancePurchaseDays(request.getAdvancePurchaseDays());
        existing.recalculateTotal();

        // Update embedded benefits
        SeatBenefits sb = existing.getSeatBenefits();
        if (request.getExtraSeatSpace() != null) sb.setExtraSeatSpace(request.getExtraSeatSpace());
        if (request.getPreferredSeatChoice() != null) sb.setPreferredSeatChoice(request.getPreferredSeatChoice());
        if (request.getAdvanceSeatSelection() != null) sb.setAdvanceSeatSelection(request.getAdvanceSeatSelection());
        if (request.getGuaranteedSeatTogether() != null) sb.setGuaranteedSeatTogether(request.getGuaranteedSeatTogether());

        BoardingBenefits bb = existing.getBoardingBenefits();
        if (request.getPriorityBoarding() != null) bb.setPriorityBoarding(request.getPriorityBoarding());
        if (request.getPriorityCheckin() != null) bb.setPriorityCheckin(request.getPriorityCheckin());
        if (request.getFastTrackSecurity() != null) bb.setFastTrackSecurity(request.getFastTrackSecurity());

        InFlightBenefits ifb = existing.getInFlightBenefits();
        if (request.getComplimentaryMeals() != null) ifb.setComplimentaryMeals(request.getComplimentaryMeals());
        if (request.getPremiumMealChoice() != null) ifb.setPremiumMealChoice(request.getPremiumMealChoice());
        if (request.getInFlightInternet() != null) ifb.setInFlightInternet(request.getInFlightInternet());
        if (request.getInFlightEntertainment() != null) ifb.setInFlightEntertainment(request.getInFlightEntertainment());
        if (request.getComplimentaryBeverages() != null) ifb.setComplimentaryBeverages(request.getComplimentaryBeverages());

        FlexibilityBenefits fb = existing.getFlexibilityBenefits();
        if (request.getFreeDateChange() != null) fb.setFreeDateChange(request.getFreeDateChange());
        if (request.getPartialRefund() != null) fb.setPartialRefund(request.getPartialRefund());
        if (request.getFullRefund() != null) fb.setFullRefund(request.getFullRefund());

        PremiumServiceBenefits psb = existing.getPremiumServiceBenefits();
        if (request.getLoungeAccess() != null) psb.setLoungeAccess(request.getLoungeAccess());
        if (request.getAirportTransfer() != null) psb.setAirportTransfer(request.getAirportTransfer());
    }

    public static PassengerType passengerTypeOf(FareRequest request) {
        return request.getPassengerType() != null ? request.getPassengerType() : PassengerType.ADT;
    }

    private static boolean bool(Boolean value) {
        return value != null ? value : false;
    }
}
