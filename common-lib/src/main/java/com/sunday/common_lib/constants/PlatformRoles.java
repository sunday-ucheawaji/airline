package com.sunday.common_lib.constants;

import java.util.List;
import java.util.Set;

/**
 * Names of the platform-level roles (scope PLATFORM, or BASELINE for the applicant). Roles are data seeded by
 * user-service's Flyway migration; this class is the single source of truth for the exact strings.
 */
public final class PlatformRoles {

    private PlatformRoles() {}

    /** Holds every permission (incl. ACCESS_MANAGE); the only role that can grant or revoke itself. */
    public static final String SUPER_ADMIN = "SUPER_ADMIN";

    /** BASELINE scope: auto-granted to every non-staff user; never counts as a platform role. */
    public static final String AIRLINE_APPLICANT = "AIRLINE_APPLICANT";

    public static final String ONBOARDING_OFFICER = "ONBOARDING_OFFICER";
    public static final String COMPLIANCE_OFFICER = "COMPLIANCE_OFFICER";
    public static final String COMMERCIAL_OFFICER = "COMMERCIAL_OFFICER";
    public static final String TECHNICAL_OFFICER = "TECHNICAL_OFFICER";
    public static final String SENIOR_APPROVER = "SENIOR_APPROVER";
    public static final String AIRLINE_PROVISIONER = "AIRLINE_PROVISIONER";
    public static final String GDS_PLATFORM_ADMIN = "GDS_PLATFORM_ADMIN";
    public static final String GDS_AUDITOR = "GDS_AUDITOR";

    /** Every staff role except SUPER_ADMIN. */
    private static final Set<String> STAFF_ROLES = Set.of(
            ONBOARDING_OFFICER, COMPLIANCE_OFFICER, COMMERCIAL_OFFICER, TECHNICAL_OFFICER,
            SENIOR_APPROVER, AIRLINE_PROVISIONER, GDS_PLATFORM_ADMIN, GDS_AUDITOR);

    /**
     * Separation of duties: a user may hold at most one role from any pair listed here together.
     * Each entry is {@code {roleA, roleB}} (order-insensitive).
     */
    public static final List<Set<String>> CONFLICTING_ROLE_PAIRS = buildConflicts();

    /** True when granting {@code candidate} to a user who already holds {@code held} would break separation of duties. */
    public static boolean conflicts(String candidate, String held) {
        return CONFLICTING_ROLE_PAIRS.contains(Set.of(candidate, held));
    }

    private static List<Set<String>> buildConflicts() {
        var pairs = new java.util.ArrayList<Set<String>>();
        pairs.add(Set.of(SENIOR_APPROVER, AIRLINE_PROVISIONER));
        for (String reviewer : List.of(ONBOARDING_OFFICER, COMPLIANCE_OFFICER, COMMERCIAL_OFFICER, TECHNICAL_OFFICER)) {
            pairs.add(Set.of(SENIOR_APPROVER, reviewer));
        }
        for (String other : STAFF_ROLES) {
            if (!other.equals(GDS_AUDITOR)) {
                pairs.add(Set.of(GDS_AUDITOR, other));
            }
        }
        return List.copyOf(pairs);
    }
}
