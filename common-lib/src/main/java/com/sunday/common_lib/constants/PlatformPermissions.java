package com.sunday.common_lib.constants;

/**
 * Permission names for platform-level (non-airline) access. These are data (Permission rows seeded by
 * user-service's Flyway migration), not code — this class is the single source of truth for the exact strings
 * the seed, the gateway's rules and the services' own checks must agree on.
 * Names marked "reserved" are seeded but no endpoint enforces them yet.
 */
public final class PlatformPermissions {

    private PlatformPermissions() {}

    // ---------- Applicant (own application) ----------
    public static final String ONBOARDING_APPLICATION_CREATE = "ONBOARDING_APPLICATION_CREATE";
    public static final String ONBOARDING_APPLICATION_READ_OWN = "ONBOARDING_APPLICATION_READ_OWN";
    public static final String ONBOARDING_APPLICATION_UPDATE_OWN = "ONBOARDING_APPLICATION_UPDATE_OWN";
    public static final String ONBOARDING_APPLICATION_SUBMIT = "ONBOARDING_APPLICATION_SUBMIT";
    public static final String ONBOARDING_APPLICATION_WITHDRAW = "ONBOARDING_APPLICATION_WITHDRAW"; // reserved
    public static final String DOCUMENT_UPLOAD_OWN = "DOCUMENT_UPLOAD_OWN"; // reserved
    public static final String DOCUMENT_READ_OWN = "DOCUMENT_READ_OWN"; // reserved

    // ---------- Onboarding staff ----------
    public static final String ONBOARDING_APPLICATION_READ = "ONBOARDING_APPLICATION_READ";
    public static final String ONBOARDING_APPLICATION_RETURN = "ONBOARDING_APPLICATION_RETURN";
    public static final String ONBOARDING_APPLICATION_REVIEW = "ONBOARDING_APPLICATION_REVIEW"; // reserved
    public static final String ONBOARDING_APPLICATION_ASSIGN = "ONBOARDING_APPLICATION_ASSIGN"; // reserved
    public static final String ONBOARDING_APPLICATION_COMMENT = "ONBOARDING_APPLICATION_COMMENT"; // reserved
    public static final String ONBOARDING_APPLICATION_REQUEST_INFORMATION = "ONBOARDING_APPLICATION_REQUEST_INFORMATION"; // reserved
    public static final String DOCUMENT_READ = "DOCUMENT_READ"; // reserved
    public static final String DOCUMENT_VERIFY = "DOCUMENT_VERIFY"; // reserved

    // ---------- Compliance ----------
    public static final String DOCUMENT_REJECT = "DOCUMENT_REJECT"; // reserved
    public static final String KYC_REVIEW = "KYC_REVIEW"; // reserved
    public static final String KYC_APPROVE = "KYC_APPROVE"; // reserved
    public static final String KYC_REJECT = "KYC_REJECT"; // reserved
    public static final String KYC_REQUEST_INFORMATION = "KYC_REQUEST_INFORMATION"; // reserved
    public static final String COMPLIANCE_COMMENT = "COMPLIANCE_COMMENT"; // reserved

    // ---------- Commercial ----------
    public static final String COMMERCIAL_REVIEW = "COMMERCIAL_REVIEW"; // reserved
    public static final String COMMERCIAL_APPROVE = "COMMERCIAL_APPROVE"; // reserved
    public static final String COMMERCIAL_REJECT = "COMMERCIAL_REJECT"; // reserved
    public static final String COMMERCIAL_REQUEST_INFORMATION = "COMMERCIAL_REQUEST_INFORMATION"; // reserved
    public static final String COMMERCIAL_COMMENT = "COMMERCIAL_COMMENT"; // reserved
    public static final String CONTRACT_READ = "CONTRACT_READ"; // reserved
    public static final String CONTRACT_VERIFY = "CONTRACT_VERIFY"; // reserved

    // ---------- Technical ----------
    public static final String TECHNICAL_REVIEW = "TECHNICAL_REVIEW"; // reserved
    public static final String TECHNICAL_APPROVE = "TECHNICAL_APPROVE"; // reserved
    public static final String TECHNICAL_REJECT = "TECHNICAL_REJECT"; // reserved
    public static final String TECHNICAL_REQUEST_INFORMATION = "TECHNICAL_REQUEST_INFORMATION"; // reserved
    public static final String INTEGRATION_CONFIG_READ = "INTEGRATION_CONFIG_READ"; // reserved
    public static final String INTEGRATION_CONFIG_CREATE = "INTEGRATION_CONFIG_CREATE"; // reserved
    public static final String INTEGRATION_TEST = "INTEGRATION_TEST"; // reserved

    // ---------- Senior approval ----------
    public static final String ONBOARDING_FINAL_APPROVE = "ONBOARDING_FINAL_APPROVE";
    public static final String ONBOARDING_FINAL_REJECT = "ONBOARDING_FINAL_REJECT";
    public static final String ONBOARDING_REQUEST_INFORMATION = "ONBOARDING_REQUEST_INFORMATION"; // reserved
    public static final String ONBOARDING_COMMENT = "ONBOARDING_COMMENT"; // reserved

    // ---------- Airline provisioning ----------
    public static final String AIRLINE_CREATE = "AIRLINE_CREATE";
    public static final String AIRLINE_ADMIN_ASSIGN = "AIRLINE_ADMIN_ASSIGN";
    public static final String AIRLINE_ACTIVATE = "AIRLINE_ACTIVATE";
    public static final String AIRLINE_UPDATE = "AIRLINE_UPDATE"; // reserved at platform level
    public static final String AIRLINE_DEACTIVATE = "AIRLINE_DEACTIVATE"; // reserved
    public static final String TENANT_CREATE = "TENANT_CREATE"; // reserved
    public static final String TENANT_CONFIGURE = "TENANT_CONFIGURE"; // reserved
    public static final String AIRLINE_ADMIN_CREATE = "AIRLINE_ADMIN_CREATE"; // reserved
    public static final String AIRLINE_INTEGRATION_CONFIGURE = "AIRLINE_INTEGRATION_CONFIGURE"; // reserved
    public static final String AIRLINE_CREDENTIAL_CREATE = "AIRLINE_CREDENTIAL_CREATE"; // reserved

    // ---------- Platform administration ----------
    public static final String USER_READ = "USER_READ";
    public static final String USER_ROLE_ASSIGN = "USER_ROLE_ASSIGN";
    public static final String USER_ROLE_REVOKE = "USER_ROLE_REVOKE";
    /** Define roles and permissions and assign permissions to roles. Held by SUPER_ADMIN only. */
    public static final String ACCESS_MANAGE = "ACCESS_MANAGE";
    public static final String AIRLINE_READ = "AIRLINE_READ";
    public static final String AIRLINE_SUSPEND = "AIRLINE_SUSPEND";
    public static final String AIRLINE_BAN = "AIRLINE_BAN";
    public static final String LOCATION_MANAGE = "LOCATION_MANAGE";
    public static final String USER_CREATE = "USER_CREATE"; // reserved
    public static final String USER_DISABLE = "USER_DISABLE"; // reserved
    public static final String TENANT_READ = "TENANT_READ"; // reserved
    public static final String TENANT_UPDATE = "TENANT_UPDATE"; // reserved
    public static final String SYSTEM_CONFIGURATION_UPDATE = "SYSTEM_CONFIGURATION_UPDATE"; // reserved

    // ---------- Audit ----------
    public static final String APPROVAL_HISTORY_READ = "APPROVAL_HISTORY_READ";
    public static final String AUDIT_LOG_READ = "AUDIT_LOG_READ"; // reserved
    public static final String DOCUMENT_AUDIT_READ = "DOCUMENT_AUDIT_READ"; // reserved
}
