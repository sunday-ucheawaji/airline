package com.sunday.services.model;

import com.sunday.services.enums.IdentityChangeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * A member's request to change an airline's compliance-verified identity fields. A null proposed field means
 * "unchanged". Applied to the airline only when platform staff approve it.
 */
@Entity
@Table(name = "airline_identity_change_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AirlineIdentityChangeRequest {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airline_id", nullable = false, updatable = false)
    private Airline airline;

    @Column(name = "requested_by_user_id", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID requestedByUserId;

    @Column(name = "proposed_legal_name")
    private String proposedLegalName;

    @Column(name = "proposed_registration_number", length = 100)
    private String proposedRegistrationNumber;

    @Column(name = "proposed_iata_code", length = 2)
    private String proposedIataCode;

    @Column(name = "proposed_icao_code", length = 3)
    private String proposedIcaoCode;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IdentityChangeStatus status;

    @Column(name = "reviewed_by_user_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID reviewedByUserId;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
