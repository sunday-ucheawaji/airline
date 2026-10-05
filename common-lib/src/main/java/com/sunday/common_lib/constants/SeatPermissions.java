package com.sunday.common_lib.constants;

/**
 * Permission names seat-service checks for write/read access, scoped per airline membership
 * (same convention as {@link AircraftPermissions}/{@link AncillaryPermissions}/{@link FlightOpsPermissions}).
 * These are data (Permission rows), not code — this class is only the single source of truth
 * for the exact strings both the seeding step and seat-service's checks must agree on.
 * <p>
 * Split into a structural/operational pair (mirroring real airline PSS practice — cabin/fleet
 * configuration vs. inventory control are distinct operational functions), not a single
 * SEAT_MANAGE/SEAT_READ pair, so each can be granted to a narrower role independently.
 */
public final class SeatPermissions {

    private SeatPermissions() {}

    /** Create/update/delete CabinClass, SeatMap, and Seat (structural cabin/seat-map configuration). Granted to OWNER+ADMIN+SEAT_CONFIG_OFFICER. */
    public static final String SEAT_CONFIG_MANAGE = "SEAT_CONFIG_MANAGE";

    /** Read CabinClass/SeatMap/Seat/FlightInstanceCabin/SeatInstance data. Granted to OWNER+ADMIN+SEAT_CONFIG_OFFICER+INVENTORY_CONTROL_OFFICER. */
    public static final String SEAT_READ = "SEAT_READ";

    /** Create/update/delete a FlightInstanceCabin, and directly create a SeatInstance (per-departure cabin/inventory provisioning). Granted to OWNER+ADMIN+INVENTORY_CONTROL_OFFICER. */
    public static final String SEAT_INSTANCE_CONFIG_MANAGE = "SEAT_INSTANCE_CONFIG_MANAGE";

    /** Forward-only seat-status transitions (AVAILABLE->BOOKED/BLOCKED, BOOKED->OCCUPIED) — the operational inventory lever. Granted to OWNER+ADMIN+INVENTORY_CONTROL_OFFICER. */
    public static final String SEAT_INSTANCE_STATUS_MANAGE = "SEAT_INSTANCE_STATUS_MANAGE";

    /** Reversal of a seat status back to AVAILABLE (cancellation, unblock, post-flight reset) — kept separate from SEAT_INSTANCE_STATUS_MANAGE so "give inventory back" is an explicit, separately-audited action. Granted to OWNER+ADMIN+INVENTORY_CONTROL_OFFICER. */
    public static final String SEAT_INSTANCE_RELEASE = "SEAT_INSTANCE_RELEASE";
}
