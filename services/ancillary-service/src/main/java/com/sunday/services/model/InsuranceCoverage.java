package com.sunday.services.model;

import com.sunday.common_lib.enums.CoverageType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(name = "insurance_coverages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceCoverage {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    // Same bounded context: Ancillary lives in this service
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ancillary_id", nullable = false)
    private Ancillary ancillary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CoverageType coverageType;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private BigDecimal coverageAmount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "INR";

    @Builder.Default
    private boolean isFlat = true;

    @Column(length = 500)
    private String claimCondition;

    @Column(length = 100)
    private String emergencyContact;

    private Integer displayOrder;

    @Builder.Default
    private boolean active = true;
}
