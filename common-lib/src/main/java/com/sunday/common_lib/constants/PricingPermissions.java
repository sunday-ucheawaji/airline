package com.sunday.common_lib.constants;

/**
 * Permission names pricing-service checks for fare, fare-rule and baggage-policy access, scoped per
 * airline membership. These are data (Permission rows), not code — this class is only the single
 * source of truth for the exact strings both the seeding step and pricing-service's checks must agree on.
 */
public final class PricingPermissions {

    private PricingPermissions() {}

    /** Read fares, fare rules and baggage policies for an airline. Granted to OWNER+ADMIN+REVENUE_MANAGER. */
    public static final String FARE_READ = "FARE_READ";

    /** Create, update, publish, withdraw and delete fares and baggage policies. Granted to OWNER+ADMIN+REVENUE_MANAGER. */
    public static final String FARE_MANAGE = "FARE_MANAGE";

    /** Create, update and delete fare rules (refund/change terms, which are liability). Granted to OWNER only. */
    public static final String FARE_RULES_MANAGE = "FARE_RULES_MANAGE";
}
