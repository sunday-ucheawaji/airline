package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.enums.PaymentGateway;
import com.sunday.common_lib.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {

    private UUID id;
    private UUID userId;
    private String userName;
    private String userEmail;
    private UUID bookingId;
    private UUID airlineId;
    private String refundId;
    private PaymentStatus status;
    private PaymentGateway gateway;
    private BigDecimal amount;
    private String transactionId;
    private String gatewayPaymentId;
    private String gatewayOrderId;
    private String gatewaySignature;
    private String paymentMethod;
    private String description;
    private String failureReason;
    private Integer retryCount;
    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;
    private Boolean notificationSent;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
