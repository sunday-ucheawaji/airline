package com.sunday.services.enums;

public enum RoleScope {
    /** Platform-wide staff authority, granted directly to a user (see UserPlatformRole). */
    PLATFORM,
    /** Scoped to a specific airline, granted through AirlineMembership in airline-core-service. */
    AIRLINE,
    /** Granted implicitly to every user who holds no PLATFORM role (e.g. AIRLINE_APPLICANT); never assigned directly. */
    BASELINE
}
