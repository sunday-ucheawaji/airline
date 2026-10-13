package com.sunday.common_lib.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent {
    private UUID paymentId;
    private UUID bookingId;
    private UUID userId;
    private BigDecimal amount;
    private String currency;
    private String transactionId;
    private String failureReason;
    private LocalDateTime failedAt;

    // Resolved by the publisher (payment-service already has UserClient) so downstream consumers
    // never need their own cross-service call just to know who to notify.
    private String contactEmail;
    private String contactPhone;
}
