package com.sunday.services.model;

import com.sunday.services.enums.DocumentStatus;
import com.sunday.services.enums.DocumentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Metadata of a document uploaded for an onboarding application. The file itself lives in private object storage
 * (never in the database): first in the quarantine area, and in the clean area once the automatic checks pass.
 * {@code storageBucket}/{@code storageKey} always point at wherever the object currently is.
 */
@Entity
@Table(name = "onboarding_documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private AirlineOnboardingApplication application;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private DocumentType documentType;

    // Cleaned for display only; never used to build a storage key or a header.
    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    // What the uploader claimed. Untrusted: kept only to detect a mismatch with the real type.
    @Column(name = "declared_content_type", length = 150)
    private String declaredContentType;

    @Column(name = "storage_provider", nullable = false, length = 50)
    private String storageProvider;

    @Column(name = "storage_bucket", nullable = false)
    private String storageBucket;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    // Found by inspecting the file itself, once processing finishes.
    @Column(name = "detected_content_type", length = 150)
    private String detectedContentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PROCESSING;

    @Column(name = "blocked_reason", length = 500)
    private String blockedReason;

    // How many times the sweeper had to republish this upload because processing never finished.
    @Column(name = "processing_attempts", nullable = false)
    @Builder.Default
    private int processingAttempts = 0;

    // Logical cross-service references to user-service's User — not physical FKs.
    @Column(name = "uploaded_by_user_id", nullable = false)
    private Long uploadedByUserId;

    @Column(name = "verified_by_user_id")
    private Long verifiedByUserId;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
