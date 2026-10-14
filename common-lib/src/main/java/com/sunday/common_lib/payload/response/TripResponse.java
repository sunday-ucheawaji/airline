package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.common_lib.enums.TripType;
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
public class TripResponse {

    private UUID id;
    private String tripReference;
    private TripType tripType;
    private BookingStatus status;
    private String currency;
    private BigDecimal totalAmount;
    private UUID paymentId;
    private LocalDateTime createdAt;
    /** One booking per flight, in travel order. */
    private List<BookingResponse> bookings;
}
