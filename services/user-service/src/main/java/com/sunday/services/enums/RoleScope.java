package com.sunday.services.enums;

public enum RoleScope {
    /** Platform-wide authority, granted directly to a user (see UserPlatformRole) — e.g. reviewing onboarding applications. */
    PLATFORM,
    /** Scoped to a specific airline, meant to be granted via AirlineMembership (airline-core-service, not yet built). */
    AIRLINE
}
