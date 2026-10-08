package com.sunday.services.model;

import com.sunday.common_lib.enums.PaymentGateway;
import com.sunday.common_lib.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cross-service ref: User (user-service)
    @Column(name = "user_id")
    private Long userId;

    // Cross-service ref: Booking (booking-service)
    @Column(name = "booking_id")
    private Long bookingId;

    // Cross-service ref: Airline (airline-core-service). Denormalized at initiatePayment time from the
    // booking's own airlineId (same snapshot-at-creation pattern as Booking's fare fields) so every
    // airline-scoped permission check has it in hand without a Feign round trip per check.
    @Column(name = "airline_id")
    private Long airlineId;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentGateway provider;

    private String providerPaymentId;
    private String transactionId;
    private String method;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String failureReason;
    private LocalDateTime paidAt;
    private String refundId;

    // Correlates back to the caller's own saga/attempt (booking-service passes its bookingReference).
    // Lets a retried initiatePayment Feign call return the original response instead of a second row.
    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Lob
    @Column(name = "initiate_response_json")
    private String initiateResponseJson;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
