package com.sunday.services.processing;

/**
 * Result of inspecting a file's structure: either the bytes to keep (unchanged for a PDF, re-encoded for an image)
 * or the reason it must be blocked.
 */
public record InspectionOutcome(byte[] content, String blockedReason) {

    public static InspectionOutcome accepted(byte[] content) {
        return new InspectionOutcome(content, null);
    }

    public static InspectionOutcome blocked(String reason) {
        return new InspectionOutcome(null, reason);
    }

    public boolean isBlocked() {
        return blockedReason != null;
    }
}
