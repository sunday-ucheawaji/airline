package com.sunday.services.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(name = "flight_cabin_ancillaries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_flight_cabin_ancillary", columnNames = {"flight_id", "cabin_class_id", "ancillary_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightCabinAncillary {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    // Cross-service reference: Flight lives in flight-ops-service
    @Column(name = "flight_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID flightId;

    // Cross-service reference: CabinClass lives in seat-service
    @Column(name = "cabin_class_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID cabinClassId;

    // Same bounded context: Ancillary lives in this service
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ancillary_id", nullable = false)
    private Ancillary ancillary;

    @Column(nullable = false)
    @Builder.Default
    private Boolean available = true;

    private Integer maxQuantity;

    private BigDecimal price;

    private String currency;

    @Column(nullable = false)
    @Builder.Default
    private Boolean includedInFare = false;
}
