package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {

    private UUID id;
    private String ticketNumber;
    private TicketStatus status;
    private LocalDateTime issuedAt;

    // Booking details
    private UUID bookingId;
    private String bookingReference;

    // Passenger details
    private UUID passengerId;
    private String passengerFirstName;
    private String passengerLastName;
    private String passengerEmail;

    // Payment details
    private UUID paymentId;
    private Double paymentAmount;
    private String paymentCurrency;
}
