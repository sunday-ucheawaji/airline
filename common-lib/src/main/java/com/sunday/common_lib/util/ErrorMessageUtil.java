package com.sunday.common_lib.util;

/**
 * Centralized error/validation message strings, so the same wording is reused
 * everywhere instead of being retyped (and drifting) at each call site.
 * <p>
 * Messages with a {@code %s} placeholder are templates — format them with
 * {@link String#format(String, Object...)} at the call site. The rest are
 * plain constants, safe to reference directly in exception constructors or
 * as Bean Validation {@code message} attributes (compile-time constants).
 */
public class ErrorMessageUtil {

    private ErrorMessageUtil() {}

    // ---------- User ----------
    public static final String USER_NOT_FOUND_BY_EMAIL = "User not found with email: %s";
    public static final String USER_NOT_FOUND_BY_ID = "User not found with id: %s";
    public static final String EMAIL_ALREADY_REGISTERED = "Email already registered";
    public static final String INVALID_CREDENTIALS = "Invalid email or password";
    public static final String EMAIL_NOT_VERIFIED = "Please verify your email before logging in";
    public static final String ACCOUNT_LOCKED = "Account is temporarily locked due to repeated failed login attempts. Try again later.";
    public static final String ACCOUNT_NOT_ACTIVE = "Account is not active";

    // ---------- Email verification ----------
    public static final String INVALID_VERIFICATION_TOKEN = "Invalid verification token";
    public static final String VERIFICATION_TOKEN_ALREADY_USED = "Verification token has already been used";
    public static final String VERIFICATION_TOKEN_EXPIRED = "Verification token has expired";

    // ---------- Refresh tokens ----------
    public static final String INVALID_REFRESH_TOKEN = "Invalid refresh token";
    public static final String REFRESH_TOKEN_EXPIRED = "Refresh token has expired";
    public static final String REFRESH_TOKEN_REUSE_DETECTED =
            "Refresh token has already been used; all sessions have been revoked";

    // ---------- Roles & Permissions ----------
    public static final String ROLE_NOT_FOUND_BY_ID = "Role not found with id: %s";
    public static final String ROLE_ALREADY_EXISTS = "Role with name %s already exists";
    public static final String PERMISSION_NOT_FOUND_BY_ID = "Permission not found with id: %s";
    public static final String PERMISSION_ALREADY_EXISTS = "Permission with name %s already exists";
    public static final String PERMISSION_NOT_ASSIGNED_TO_ROLE = "Permission %s is not assigned to role %s";

    // ---------- Bean validation: fields ----------
    public static final String FIRST_NAME_MANDATORY = "firstName is mandatory";
    public static final String LAST_NAME_MANDATORY = "lastName is mandatory";
    public static final String EMAIL_MANDATORY = "Email is mandatory";
    public static final String EMAIL_INVALID = "Email should be valid";
    public static final String NAME_MANDATORY = "name is mandatory";
    public static final String STATUS_MANDATORY = "status is mandatory";
    public static final String ROLE_MANDATORY = "role is mandatory";
    public static final String PERMISSION_MANDATORY = "permission is mandatory";
    public static final String USER_MANDATORY = "user is mandatory";
    public static final String TOKEN_HASH_MANDATORY = "tokenHash is mandatory";
    public static final String EXPIRES_AT_MANDATORY = "expiresAt is mandatory";
    public static final String PASSWORD_MIN_LENGTH = "Password must be at least 8 characters";
}
