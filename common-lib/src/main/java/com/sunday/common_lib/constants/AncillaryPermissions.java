package com.sunday.common_lib.constants;

/**
 * Permission names ancillary-service checks for write access, scoped per airline membership.
 * These are data (Permission rows), not code — this class is only the single source of truth
 * for the exact string both the seeding step and ancillary-service's checks must agree on.
 */
public final class AncillaryPermissions {

    private AncillaryPermissions() {}

    /** Manage baggage/travel-protection/meal catalogue and their flight/cabin offerings. Granted to OWNER+ADMIN. */
    public static final String MANAGE = "ANCILLARY_MANAGE";

    /** Manage InsuranceCoverage terms (payout amounts, claim conditions) — financial liability, OWNER only. */
    public static final String INSURANCE_MANAGE = "ANCILLARY_INSURANCE_MANAGE";
}
