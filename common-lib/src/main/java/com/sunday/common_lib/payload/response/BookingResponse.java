package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.embeddable.ContactInfo;
import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.common_lib.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private UUID id;
    private String bookingReference;

    private UUID userId;
    private String userName;
    private String userEmail;

    private UUID flightId;
    private String flightNumber;
    private String flightName;
    private String departureAirport;
    private String arrivalAirport;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;

    private BookingStatus status;
    private LocalDateTime bookingDate;
    private LocalDateTime lastModified;

    private List<PassengerResponse> passengers;
    private List<SeatInstanceResponse> seatInstances;
    private PaymentLinkResponse payment;
    private List<FlightCabinAncillaryResponse> ancillaries;
    private List<FlightMealResponse> meals;
    private List<TicketResponse> tickets;

    // Payment details
    private PaymentStatus paymentStatus;
    private String paymentLink;

    private UUID fareId;
    private String fareName;
    private BigDecimal fareBaseFare;
    private BigDecimal fareTaxesAndFees;
    private BigDecimal fareAirlineFees;

    private FareResponse fare;

    private Integer totalPassengers;
    private BigDecimal totalAmount;
    private BigDecimal refundAmount;
    private String currency;

    private String specialRequests;
    private Boolean requiresWheelchairAssistance;
    private Boolean requiresSpecialMeals;

    // Flight duration and other derived information
    private String flightDuration;
    private Boolean isUpcoming;
    private Boolean isPast;

    private String airlineName;
    private String airlineLogo;

    private ContactInfo contactInfo;
}
