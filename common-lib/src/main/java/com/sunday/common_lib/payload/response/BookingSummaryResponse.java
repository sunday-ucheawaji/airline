package com.sunday.common_lib.payload.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSummaryResponse {
    private UUID id;
    private String bookingReference;
    private String status;
    private Double totalAmount;
    private Instant bookingDate;
}
