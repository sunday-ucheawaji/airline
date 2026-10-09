package com.sunday.common_lib.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent {
    private Long paymentId;
    private Long bookingId;
    private Long userId;
    private BigDecimal amount;
    private String transactionId;
    private String failureReason;
    private LocalDateTime failedAt;

    // Resolved by the publisher (payment-service already has UserClient) so downstream consumers
    // never need their own cross-service call just to know who to notify.
    private String contactEmail;
    private String contactPhone;
}
