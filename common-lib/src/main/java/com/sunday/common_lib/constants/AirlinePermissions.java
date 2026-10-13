package com.sunday.common_lib.constants;

/**
 * Permission names airline-core-service checks for writes to an airline's own profile, scoped per airline
 * membership (same convention as {@link AircraftPermissions} / {@link AirlineMembershipPermissions}).
 */
public final class AirlinePermissions {

    private AirlinePermissions() {}

    /** Edit the airline's profile. Granted to OWNER+ADMIN. */
    public static final String PROFILE_UPDATE = "AIRLINE_PROFILE_UPDATE";

    /** Close (soft-delete) the airline. Granted to OWNER only. */
    public static final String CLOSE = "AIRLINE_CLOSE";

    /** Ask platform staff to change the airline's verified identity fields. Granted to OWNER only. */
    public static final String IDENTITY_CHANGE_REQUEST = "AIRLINE_IDENTITY_CHANGE_REQUEST";
}
