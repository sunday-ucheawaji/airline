package com.sunday.services.model;

import com.sunday.common_lib.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Extras bought for a confirmed booking after its purchase, with their own payment (whose id is the order's). */
@Entity
@Table(name = "extras_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtrasOrder {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID bookingId;

    @Column(name = "user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID userId;

    @Column(name = "airline_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID airlineId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal refundAmount;

    // The client's Idempotency-Key, unique per booking: a retried request returns this order instead of a new one.
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "payment_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID paymentId;

    // Loaded with the order (small lists, one select each), so responses can map them outside a transaction.
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @CollectionTable(name = "extras_order_ancillaries", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "ancillary_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    @Builder.Default
    private List<UUID> ancillaryIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @CollectionTable(name = "extras_order_meals", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "meal_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    @Builder.Default
    private List<UUID> mealIds = new ArrayList<>();

    @Version
    private Long version;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
