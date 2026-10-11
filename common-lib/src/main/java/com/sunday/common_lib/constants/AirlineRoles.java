package com.sunday.common_lib.constants;

import java.util.Set;

/**
 * Names of the AIRLINE-scope roles (scope AIRLINE). Roles are data seeded by user-service's Flyway
 * migration; this class is the single source of truth for the exact strings, letting callers resolve
 * a role by name (via RoleClient) instead of depending on a fixed, externally-configured id.
 */
public final class AirlineRoles {

    private AirlineRoles() {}

    public static final String OWNER = "OWNER";
    public static final String ADMIN = "ADMIN";
    public static final String VIEWER = "VIEWER";
    public static final String FLIGHT_DISPATCHER = "FLIGHT_DISPATCHER";
    public static final String FLEET_ASSIGNMENT_OFFICER = "FLEET_ASSIGNMENT_OFFICER";
    public static final String SEAT_CONFIG_OFFICER = "SEAT_CONFIG_OFFICER";
    public static final String INVENTORY_CONTROL_OFFICER = "INVENTORY_CONTROL_OFFICER";
    public static final String REVENUE_MANAGER = "REVENUE_MANAGER";
    public static final String RESERVATIONS_OFFICER = "RESERVATIONS_OFFICER";
    public static final String TICKETING_OFFICER = "TICKETING_OFFICER";
    public static final String REFUND_APPROVER = "REFUND_APPROVER";

    /** Every invitable role except OWNER, which is granted only at airline provisioning. */
    public static final Set<String> INVITABLE_ROLES = Set.of(
            ADMIN, VIEWER, FLIGHT_DISPATCHER, FLEET_ASSIGNMENT_OFFICER, SEAT_CONFIG_OFFICER,
            INVENTORY_CONTROL_OFFICER, REVENUE_MANAGER, RESERVATIONS_OFFICER, TICKETING_OFFICER, REFUND_APPROVER);
}
