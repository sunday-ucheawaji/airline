package com.sunday.services.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(name = "fare_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class FareRules {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    private String ruleName;

    // Cross-service ref: Airline is in airline-core-service. Copied from the fare, never from the request.
    @Column(name = "airline_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID airlineId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fare_id", nullable = false)
    @JsonIgnore
    private Fare fare;

    private Boolean isRefundable;

    @Column(name = "change_fee")
    private BigDecimal changeFee;

    @Column(name = "cancellation_fee")
    private BigDecimal cancellationFee;

    @Column(name = "refund_deadline_days")
    private Integer refundDeadlineDays;

    @Column(name = "change_deadline_hours")
    private Integer changeDeadlineHours;

    @Builder.Default
    private Boolean isChangeable = false;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_by")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID createdBy;

    @Column(name = "updated_by")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID updatedBy;

    @Column(updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    public void preCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
