package com.sunday.common_lib.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Published to {@link #TOPIC} once an onboarding document has been saved to quarantine. The asynchronous processor
 * picks it up, inspects the file's real contents and either promotes it to clean storage or blocks it.
 * Carries only identifiers and what the uploader claimed; the file itself never travels through Kafka.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingDocumentUploadedEvent {

    public static final String TOPIC = "onboarding.document.uploaded";

    private Long documentId;
    private Long applicationId;
    /** Where the raw, unchecked file sits in the quarantine area. */
    private String quarantineKey;
    /** What the uploader claimed — untrusted, used only to detect a mismatch with the real type. */
    private String declaredContentType;
    private String declaredFileName;
    private Long uploadedByUserId;
    private Instant uploadedAt;
}
