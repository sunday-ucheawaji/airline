package com.sunday.services.enums;

public enum MembershipStatus {
    INVITED,
    ACTIVE,
    SUSPENDED,
    REMOVED,
    /** A pending invitation was cancelled by the inviter before it was accepted. */
    REVOKED,
    /** A pending invitation was never accepted before its {@code expiresAt}. */
    EXPIRED
}
