package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.enums.PassengerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareRequest {

    @NotBlank(message = "Fare name is required")
    private String name;

    @NotNull(message = "RBD code is required")
    private Character rbdCode;

    @NotNull(message = "Flight ID is required")
    private UUID flightId;

    @NotNull(message = "Cabin class is required")
    private CabinClassType cabinClass;

    /** Defaults to ADT when omitted. */
    private PassengerType passengerType;

    @Size(max = 20)
    private String fareBasisCode;

    // Pricing
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a 3-letter ISO code")
    private String currency;

    @NotNull(message = "Base fare is required")
    @Positive
    private BigDecimal baseFare;

    @PositiveOrZero
    private BigDecimal taxesAndFees;

    @PositiveOrZero
    private BigDecimal airlineFees;

    @Size(max = 100)
    private String fareLabel;

    // Sales and travel windows
    @NotNull(message = "Sales start is required")
    private Instant salesFrom;

    @NotNull(message = "Sales end is required")
    private Instant salesTo;

    @NotNull(message = "Travel start date is required")
    private LocalDate travelFrom;

    @NotNull(message = "Travel end date is required")
    private LocalDate travelTo;

    @PositiveOrZero
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

    // Premium service benefits
    private Boolean loungeAccess;
    private Boolean airportTransfer;
}
