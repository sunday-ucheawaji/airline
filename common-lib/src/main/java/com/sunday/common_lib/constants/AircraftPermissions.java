package com.sunday.common_lib.constants;

/**
 * Permission names airline-core-service checks for Aircraft access, scoped per airline membership
 * (same convention as {@link AncillaryPermissions} / {@link AirlineMembershipPermissions}).
 */
public final class AircraftPermissions {

    private AircraftPermissions() {}

    /** Create/update/delete an aircraft. Granted to OWNER+ADMIN. */
    public static final String MANAGE = "AIRCRAFT_MANAGE";

    /** Read one aircraft or list an airline's fleet. Granted to OWNER+ADMIN+VIEWER. */
    public static final String READ = "AIRCRAFT_READ";
}
