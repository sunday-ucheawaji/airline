package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.enums.PassengerType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * What a customer sees for one fare on a flight. Deliberately narrower than FareResponse: no ids of staff records,
 * no version, no audit fields, no cabin reference. Only published, in-window fares are ever returned as offers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareOfferResponse {

    private UUID id;
    private String name;
    private Character rbdCode;
    private String fareLabel;
    private CabinClassType cabinClass;
    private PassengerType passengerType;
    private LocalDate travelFrom;
    private LocalDate travelTo;

    private String currency;
    private BigDecimal baseFare;
    private BigDecimal taxesAndFees;
    private BigDecimal airlineFees;
    private BigDecimal totalPrice;

    private Benefits benefits;
    private Rules rules;
    private Baggage baggage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Benefits {
        private Boolean extraSeatSpace;
        private Boolean preferredSeatChoice;
        private Boolean advanceSeatSelection;
        private Boolean guaranteedSeatTogether;
        private Boolean priorityBoarding;
        private Boolean priorityCheckin;
        private Boolean fastTrackSecurity;
        private Boolean complimentaryMeals;
        private Boolean premiumMealChoice;
        private Boolean inFlightInternet;
        private Boolean inFlightEntertainment;
        private Boolean complimentaryBeverages;
        private Boolean loungeAccess;
        private Boolean airportTransfer;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rules {
        private String ruleName;
        private Boolean isRefundable;
        private BigDecimal cancellationFee;
        private Integer refundDeadlineDays;
        private Boolean isChangeable;
        private BigDecimal changeFee;
        private Integer changeDeadlineHours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Baggage {
        private String name;
        private String description;
        private Double cabinBaggageMaxWeight;
        private Integer cabinBaggagePieces;
        private Double checkInBaggageMaxWeight;
        private Integer checkInBaggagePieces;
        private Integer freeCheckedBagsAllowance;
        private Boolean priorityBaggage;
        private Boolean extraBaggageAllowance;
    }
}
