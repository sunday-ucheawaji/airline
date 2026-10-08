package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.enums.FareStatus;
import com.sunday.common_lib.enums.PassengerType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareResponse {

    private Long id;
    private String name;
    private Character rbdCode;
    private Long airlineId;
    private FareStatus status;
    private Long flightId;
    private Long cabinClassId;
    private CabinClassType cabinClass;
    private PassengerType passengerType;
    private String fareBasisCode;

    // Pricing (amounts are in `currency`)
    private String currency;
    private BigDecimal baseFare;
    private BigDecimal taxesAndFees;
    private BigDecimal airlineFees;
    private BigDecimal totalPrice;
    private String fareLabel;

    // Sales and travel windows
    private Instant salesFrom;
    private Instant salesTo;
    private LocalDate travelFrom;
    private LocalDate travelTo;
    private Integer advancePurchaseDays;

    // Seat benefits
    private Boolean extraSeatSpace;
    private Boolean preferredSeatChoice;
    private Boolean advanceSeatSelection;
    private Boolean guaranteedSeatTogether;

    // Boarding benefits
    private Boolean priorityBoarding;
    private Boolean priorityCheckin;
    private Boolean fastTrackSecurity;

    // In-flight benefits
    private Boolean complimentaryMeals;
    private Boolean premiumMealChoice;
    private Boolean inFlightInternet;
    private Boolean inFlightEntertainment;
    private Boolean complimentaryBeverages;

    // Flexibility benefits
    private Boolean freeDateChange;
    private Boolean partialRefund;
    private Boolean fullRefund;

    // Premium service benefits
    private Boolean loungeAccess;
    private Boolean airportTransfer;

    // Relationships
    private Long fareRulesId;
    private FareRulesResponse fareRules;
    private BaggagePolicyResponse baggagePolicy;

    // Audit
    private Instant createdAt;
    private Instant updatedAt;
}
