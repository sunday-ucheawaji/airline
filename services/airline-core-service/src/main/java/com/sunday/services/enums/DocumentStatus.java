package com.sunday.services.enums;

/**
 * Lifecycle of an uploaded document. PROCESSING is the automatic inspection (asynchronous); BLOCKED is a system
 * decision that the file is unsafe; CLEAN means it passed and awaits staff; VERIFIED and REJECTED are staff decisions.
 */
public enum DocumentStatus {
    PROCESSING,
    CLEAN,
    BLOCKED,
    VERIFIED,
    REJECTED
}
