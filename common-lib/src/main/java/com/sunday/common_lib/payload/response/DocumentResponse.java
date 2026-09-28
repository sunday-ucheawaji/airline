package com.sunday.common_lib.payload.response;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse {

    private Long id;
    private Long applicationId;
    /** CERTIFICATE_OF_INCORPORATION, AIR_OPERATOR_CERTIFICATE, OPERATING_LICENSE, BUSINESS_REGISTRATION or OTHER. */
    private String documentType;
    /** The uploader's file name, cleaned; for display only. */
    private String originalFileName;
    /** PROCESSING, CLEAN, BLOCKED, VERIFIED or REJECTED. */
    private String status;
    /** The type found by inspecting the file itself; null until processing finishes. */
    private String detectedContentType;
    private Long fileSize;
    private String checksumSha256;
    /** Why the system blocked the file (BLOCKED only). */
    private String blockedReason;
    private Long uploadedByUserId;
    private Long verifiedByUserId;
    private Instant verifiedAt;
    /** Why a reviewer rejected the document (REJECTED only). */
    private String rejectionReason;
    private Instant createdAt;
}
