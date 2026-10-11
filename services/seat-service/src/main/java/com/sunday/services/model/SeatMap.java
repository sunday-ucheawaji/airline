package com.sunday.services.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(name = "seat_maps")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatMap {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private int totalRows;

    @Column(nullable = false)
    private int rightSeatsPerRow;

    @Column(nullable = false)
    private int leftSeatsPerRow;

    @Column(name = "airline_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID airlineId;

    @OneToMany(mappedBy = "seatMap", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seat> seats;

    @OneToOne
    @JoinColumn(name = "cabin_class_id")
    private CabinClass cabinClass;
}
