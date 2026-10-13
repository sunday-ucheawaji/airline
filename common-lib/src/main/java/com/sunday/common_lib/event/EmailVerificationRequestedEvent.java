package com.sunday.common_lib.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Published by user-service after a verification token is issued (sign-up or resend).
 * Carries the raw single-use token so notification-service can build the link; user-service only stores its hash.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationRequestedEvent {

    public static final String TOPIC = "user.email-verification-requested";

    /** The token row's id: unique per issuance, used by notification-service as the idempotency reference. */
    private UUID requestId;
    private UUID userId;
    private String email;
    private String firstName;
    private String token;
    private LocalDateTime expiresAt;
}
