package com.sunday.services.enums;

public enum PermissionScope {
    /** Carried in the JWT and enforced at the gateway; grantable to PLATFORM and BASELINE roles only. */
    PLATFORM,
    /** Checked per airline membership in airline-core-service; grantable to AIRLINE roles only. */
    AIRLINE
}
