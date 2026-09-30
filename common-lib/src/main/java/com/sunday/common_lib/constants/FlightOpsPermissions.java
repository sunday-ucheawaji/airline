package com.sunday.common_lib.constants;

/**
 * Permission names flight-ops-service checks for write/list access, scoped per airline
 * membership (same convention as {@link AircraftPermissions}/{@link AncillaryPermissions}).
 * These are data (Permission rows), not code — this class is only the single source of
 * truth for the exact strings both the seeding step and flight-ops-service's checks must
 * agree on.
 */
public final class FlightOpsPermissions {

    private FlightOpsPermissions() {}

    /** Create/update/delete/change-status a Flight (route/template). Granted to OWNER+ADMIN+FLIGHT_DISPATCHER. */
    public static final String FLIGHT_MANAGE = "FLIGHT_MANAGE";

    /** Read a Flight or list an airline's flights. Granted to OWNER+ADMIN+FLIGHT_DISPATCHER+FLEET_ASSIGNMENT_OFFICER+VIEWER. */
    public static final String FLIGHT_READ = "FLIGHT_READ";

    /** Create/update/delete a FlightSchedule (recurring generator). Granted to OWNER+ADMIN+FLIGHT_DISPATCHER. */
    public static final String SCHEDULE_MANAGE = "FLIGHT_SCHEDULE_MANAGE";

    /** Read a FlightSchedule. Granted to OWNER+ADMIN+FLIGHT_DISPATCHER+FLEET_ASSIGNMENT_OFFICER+VIEWER. */
    public static final String SCHEDULE_READ = "FLIGHT_SCHEDULE_READ";

    /** Create/update/delete a FlightInstance (concrete bookable departure). Granted to OWNER+ADMIN+FLIGHT_DISPATCHER. */
    public static final String INSTANCE_MANAGE = "FLIGHT_INSTANCE_MANAGE";

    /** Read a FlightInstance or list an airline's instances. Granted to OWNER+ADMIN+FLIGHT_DISPATCHER+FLEET_ASSIGNMENT_OFFICER+VIEWER. */
    public static final String INSTANCE_READ = "FLIGHT_INSTANCE_READ";

    /** Reassign the aircraft on an already-created instance — narrower than general instance management, kept separate for least-privilege delegation. Granted to OWNER+ADMIN+FLEET_ASSIGNMENT_OFFICER. */
    public static final String INSTANCE_AIRCRAFT_ASSIGN = "FLIGHT_INSTANCE_AIRCRAFT_ASSIGN";
}
