package com.sunday.common_lib.constants;

/**
 * Permission names airline-core-service checks for its own membership writes, scoped per airline membership
 * (same convention as {@link AncillaryPermissions}). Already seeded and granted in
 * {@code V8__seed_access_model.sql} (OWNER holds all five; ADMIN holds everything but REMOVE; VIEWER holds only
 * READ) — this class is just the single source of truth for the exact strings.
 */
public final class AirlineMembershipPermissions {

    private AirlineMembershipPermissions() {}

    public static final String READ = "MEMBER_READ";
    public static final String INVITE = "MEMBER_INVITE";
    public static final String UPDATE_ROLE = "MEMBER_UPDATE_ROLE";
    public static final String UPDATE_STATUS = "MEMBER_UPDATE_STATUS";
    public static final String REMOVE = "MEMBER_REMOVE";
}
