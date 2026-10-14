package com.sunday.common_lib.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Published by user-service when someone signs up with the email of an existing, verified account. Signup answers
 * the same way either way, so the account's owner is told by email instead of the person signing up.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExistingAccountSignupAttemptedEvent {

    public static final String TOPIC = "user.signup-existing-account";

    /** Unique per attempt; notification-service's idempotency reference. */
    private UUID requestId;
    private UUID userId;
    private String email;
    private String firstName;
}
