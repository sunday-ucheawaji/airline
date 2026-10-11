package com.sunday.services.model;

import com.sunday.common_lib.domain.metadata.AncillaryMetadata;
import com.sunday.common_lib.enums.AncillaryType;
import com.sunday.services.converter.AncillaryMetadataConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(name = "ancillaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ancillary {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AncillaryType type;

    @Column(length = 100)
    private String subType;

    @Column(length = 10)
    private String rfisc;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = AncillaryMetadataConverter.class)
    private AncillaryMetadata metadata;

    private Integer displayOrder;

    // Cross-service reference: Airline lives in airline-core-service
    @Column(name = "airline_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID airlineId;
}
