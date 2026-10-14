package com.sunday.services.model;

import com.sunday.common_lib.enums.CabinClassType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(name = "flight_cabin_ancillaries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_flight_cabin_ancillary", columnNames = {"flight_id", "cabin_class", "ancillary_id"})
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

    // A cabin type, not seat-service's per-aircraft cabin class id: bookings know the type, and an aircraft swap
    // changes the id but not the type.
    @Enumerated(EnumType.STRING)
    @Column(name = "cabin_class", nullable = false, length = 20)
    private CabinClassType cabinClass;

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
