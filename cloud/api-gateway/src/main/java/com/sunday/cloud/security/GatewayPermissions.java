package com.sunday.cloud.security;

/**
 * The permission names the gateway enforces. The gateway deliberately does not depend on common-lib (a servlet
 * library), so these mirror {@code PlatformPermissions} there and must be kept in step with it and with the
 * user-service seed migration.
 */
public final class GatewayPermissions {

    private GatewayPermissions() {}

    public static final String ONBOARDING_APPLICATION_CREATE = "ONBOARDING_APPLICATION_CREATE";
    public static final String ONBOARDING_APPLICATION_READ_OWN = "ONBOARDING_APPLICATION_READ_OWN";
    public static final String ONBOARDING_APPLICATION_UPDATE_OWN = "ONBOARDING_APPLICATION_UPDATE_OWN";
    public static final String ONBOARDING_APPLICATION_SUBMIT = "ONBOARDING_APPLICATION_SUBMIT";

    public static final String ONBOARDING_APPLICATION_READ = "ONBOARDING_APPLICATION_READ";
    public static final String ONBOARDING_APPLICATION_RETURN = "ONBOARDING_APPLICATION_RETURN";
    public static final String ONBOARDING_FINAL_APPROVE = "ONBOARDING_FINAL_APPROVE";
    public static final String ONBOARDING_FINAL_REJECT = "ONBOARDING_FINAL_REJECT";
    public static final String APPROVAL_HISTORY_READ = "APPROVAL_HISTORY_READ";

    public static final String AIRLINE_CREATE = "AIRLINE_CREATE";
    public static final String AIRLINE_ADMIN_ASSIGN = "AIRLINE_ADMIN_ASSIGN";
    public static final String AIRLINE_ACTIVATE = "AIRLINE_ACTIVATE";
    public static final String AIRLINE_READ = "AIRLINE_READ";
    public static final String AIRLINE_SUSPEND = "AIRLINE_SUSPEND";
    public static final String AIRLINE_BAN = "AIRLINE_BAN";

    public static final String USER_READ = "USER_READ";
    public static final String USER_ROLE_ASSIGN = "USER_ROLE_ASSIGN";
    public static final String USER_ROLE_REVOKE = "USER_ROLE_REVOKE";
    public static final String ACCESS_MANAGE = "ACCESS_MANAGE";
    public static final String LOCATION_MANAGE = "LOCATION_MANAGE";
}
