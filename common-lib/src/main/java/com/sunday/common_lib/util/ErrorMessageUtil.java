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
    public static final String ROLE_NOT_PLATFORM_SCOPED =
            "Role %s is airline-scoped and cannot be assigned directly to a user; airline-scoped roles are granted through airline membership";
    public static final String PLATFORM_ROLE_NOT_ASSIGNED_TO_USER = "Role %s is not assigned to user %s";

    // ---------- Airline ----------
    public static final String AIRLINE_NOT_FOUND_BY_ID = "Airline not found with id: %s";
    public static final String NO_ACTIVE_MEMBERSHIP_FOR_AIRLINE = "You do not have an active membership in airline %s";
    public static final String NO_PERMISSION_FOR_AIRLINE = "You do not have permission %s for airline %s";
    public static final String NO_PERMISSION_FOR_AIRLINES = "You do not have permission %s for airline(s): %s";
    public static final String AIRLINE_NOT_ACTIVE = "Airline %s is %s and cannot be modified";
    public static final String AIRLINE_IATA_CODE_ALREADY_EXISTS = "IATA code %s is already in use by another airline";
    public static final String AIRLINE_ICAO_CODE_ALREADY_EXISTS = "ICAO code %s is already in use by another airline";
    public static final String AIRLINE_STATUS_UNCHANGED = "Airline %s is already %s";
    public static final String AIRLINE_CLOSED_STATUS_LOCKED = "Airline %s has been closed and its status can no longer be changed";

    // ---------- Aircraft ----------
    public static final String AIRCRAFT_NOT_FOUND_BY_ID = "Aircraft not found with id: %s";
    public static final String AIRCRAFT_CODE_ALREADY_EXISTS = "Aircraft with code %s already exists";
    public static final String AIRCRAFT_SEATS_EXCEED_CAPACITY = "Total specified seats exceed aircraft seating capacity";
    public static final String AIRCRAFT_INVALID_YEAR_OF_MANUFACTURE = "Invalid year of manufacture";

    // ---------- Airline membership (invitations) ----------
    public static final String MEMBER_INVITE_EMAIL_MANDATORY = "A valid email is required";
    public static final String MEMBER_INVITE_ROLE_MANDATORY = "roleId is required";
    public static final String MEMBER_STATUS_MANDATORY = "status is required";
    public static final String MEMBER_INVITE_TARGET_ROLE_INVALID = "Role %s cannot be invited to an airline; only ADMIN or VIEWER can";
    public static final String MEMBER_INVITE_USER_NOT_FOUND = "No user found with email %s";
    public static final String MEMBER_INVITE_TARGET_IS_PLATFORM_STAFF = "User %s holds a platform role and cannot join an airline";
    public static final String MEMBER_ALREADY_ACTIVE = "User %s is already a member of airline %s";
    public static final String MEMBER_ALREADY_INVITED = "User %s already has a pending invitation to airline %s";
    public static final String MEMBERSHIP_NOT_FOUND = "Membership %s not found for airline %s";
    public static final String MEMBERSHIP_NOT_PENDING = "Membership %s is not a pending invitation";
    public static final String MEMBERSHIP_INVITATION_EXPIRED = "This invitation has expired";
    public static final String MEMBERSHIP_ACCEPT_NOT_INVITEE = "Only the invited user can accept this invitation";
    public static final String MEMBERSHIP_NOT_ACTIVE_OR_SUSPENDED = "Membership %s is not an active or suspended member";
    public static final String MEMBER_ROLE_UPDATE_TARGET_INVALID = "Role %s cannot be assigned to an existing member; only ADMIN or VIEWER can";
    public static final String MEMBER_STATUS_UPDATE_INVALID = "status must be ACTIVE or SUSPENDED";
    public static final String MEMBER_CANNOT_REMOVE_LAST_OWNER = "Cannot remove the last OWNER of airline %s";
    public static final String MEMBER_ROLE_UPDATE_OWNER_LOCKED = "The OWNER's role cannot be changed here";

    // ---------- Ancillary ----------
    public static final String AIRLINE_ID_REQUIRED = "airlineId is required";
    public static final String ANCILLARY_NOT_FOUND_BY_ID = "Ancillary not found with id: %s";
    public static final String ANCILLARY_NOT_AVAILABLE = "Ancillary %s is not available";
    public static final String ANCILLARY_MAX_QUANTITY_EXCEEDED = "Ancillary %s: max quantity is %s, requested %s";
    public static final String ANCILLARY_PRICE_NOT_SET = "Ancillary %s has no price set";
    public static final String MEAL_NOT_FOUND_BY_ID = "Meal not found with id: %s";
    public static final String MEAL_CODE_ALREADY_EXISTS_FOR_AIRLINE = "Meal with code %s already exists for airline %s";
    public static final String MEAL_NOT_AVAILABLE = "Meal %s is not available";
    public static final String MEAL_PRICE_NOT_SET = "Meal %s has no price set";
    public static final String FLIGHT_CABIN_ANCILLARY_NOT_FOUND_BY_ID = "FlightCabinAncillary not found with id: %s";
    public static final String FLIGHT_CABIN_ANCILLARY_NOT_FOUND_FOR_TYPE = "FlightCabinAncillary not found for type: %s";
    public static final String FLIGHT_CABIN_ANCILLARY_NOT_FOUND_FOR_IDS = "FlightCabinAncillary not found for id(s): %s";
    public static final String FLIGHT_MEAL_NOT_FOUND_BY_ID = "FlightMeal not found with id: %s";
    public static final String FLIGHT_MEAL_NOT_FOUND_FOR_IDS = "FlightMeal not found for id(s): %s";
    public static final String FLIGHT_MEAL_ALREADY_ASSIGNED_TO_FLIGHT = "Meal %s is already assigned to flight %s";
    public static final String FLIGHT_MEAL_DUPLICATE_IN_REQUEST = "Each meal can only be selected once per booking; duplicate meal id(s): %s";
    public static final String INSURANCE_COVERAGE_NOT_FOUND_BY_ID = "Insurance coverage not found with id: %s";

    // ---------- Onboarding ----------
    public static final String ONBOARDING_APPLICATION_NOT_FOUND_BY_ID = "Onboarding application not found with id: %s";
    public static final String ONBOARDING_APPLICATION_NOT_OWNED_BY_APPLICANT = "Onboarding application %s does not belong to this applicant";
    public static final String ONBOARDING_APPLICATION_NOT_EDITABLE = "Onboarding application %s is not editable in its current status: %s";
    public static final String ONBOARDING_APPLICATION_NOT_SUBMITTABLE = "Onboarding application %s cannot be submitted in its current status: %s";
    public static final String ONBOARDING_APPLICATION_NOT_REVIEWABLE = "Onboarding application %s is not awaiting review (current status: %s)";
    public static final String ONBOARDING_LEGAL_NAME_MANDATORY = "legalName is mandatory";
    public static final String ONBOARDING_DISPLAY_NAME_MANDATORY = "displayName is mandatory";
    public static final String ONBOARDING_COUNTRY_MANDATORY = "country is mandatory";
    public static final String ONBOARDING_REGISTRATION_NUMBER_MANDATORY = "registrationNumber is mandatory";
    public static final String ONBOARDING_INITIAL_ADMIN_REQUIRED_FOR_APPROVAL = "initialAdminUserId must be set before an application can be provisioned";
    public static final String ONBOARDING_DRAFTS_NOT_LISTABLE = "Draft applications are private to their applicant and cannot be listed";
    public static final String OWNER_USER_ID_MANDATORY = "ownerUserId is mandatory";
    public static final String ONBOARDING_APPLICATION_NOT_APPROVED = "Onboarding application %s must be approved before it can be provisioned (current status: %s)";
    public static final String ONBOARDING_OWNER_NOT_ASSIGNABLE = "Onboarding application %s no longer accepts an owner change (current status: %s)";
    public static final String ONBOARDING_SEGREGATION_OF_DUTIES = "You cannot perform this step on an application you %s";
    public static final String ONBOARDING_OWNER_IS_REVIEWER = "The nominated owner cannot be the user acting on application %s";
    public static final String ONBOARDING_PLATFORM_USER_CANNOT_APPLY = "Platform staff cannot apply to onboard an airline";
    public static final String ONBOARDING_PLATFORM_USER_CANNOT_OWN = "User %s is platform staff and cannot own an airline";
    public static final String ONBOARDING_OWNER_USER_NOT_FOUND = "Owner user %s does not exist";

    // ---------- Onboarding: case ownership and staged review ----------
    public static final String ONBOARDING_APPLICATION_NOT_CLAIMABLE = "Application %s can only be claimed while it is SUBMITTED (current status: %s)";
    public static final String ONBOARDING_APPLICATION_NOT_UNDER_REVIEW = "Application %s must be under review for this step (current status: %s)";
    public static final String ONBOARDING_APPLICATION_NOT_PENDING_APPROVAL = "Application %s is not awaiting final approval (current status: %s)";
    public static final String ONBOARDING_APPLICATION_NOT_WITHDRAWABLE = "Application %s cannot be withdrawn in its current status: %s";
    public static final String ONBOARDING_CASE_ALREADY_CLAIMED = "Application %s already has a case owner";
    public static final String ONBOARDING_CASE_NOT_CLAIMED = "Application %s has no case owner yet; claim it first";
    public static final String ONBOARDING_NOT_CASE_OWNER = "Only the case owner of application %s can do this";
    public static final String ONBOARDING_CASE_OWNER_MUST_BE_OFFICER = "User %s does not hold the %s role and cannot own a case";
    public static final String ONBOARDING_CASE_OWNER_IS_PARTY = "The case owner cannot be the applicant or the nominated airline owner of application %s";
    public static final String ONBOARDING_REVIEWER_IS_PARTY = "A reviewer cannot be the applicant or the nominated airline owner of application %s";
    public static final String ONBOARDING_TAKEOVER_SUPER_ADMIN_ONLY = "Only a super admin can force a takeover of a case";
    public static final String ONBOARDING_REASON_MANDATORY = "A reason is mandatory";
    public static final String ONBOARDING_COMMENT_MANDATORY_FOR_SEND_BACK = "A comment is mandatory when sending a case back";
    public static final String ONBOARDING_STAGE_UNKNOWN = "Unknown review stage: %s";
    public static final String ONBOARDING_STAGE_NOT_ASSIGNED_TO_USER = "The %s stage of application %s is not assigned to you";
    public static final String ONBOARDING_STAGE_ASSIGNEE_LACKS_ROLE = "User %s does not hold the %s role required for the %s stage";
    public static final String ONBOARDING_STAGES_NOT_APPROVED = "Application %s cannot be referred for approval: stage(s) not approved: %s";
    public static final String ONBOARDING_OPEN_INFORMATION_REQUESTS = "Application %s still has open information request(s) that must be answered first";
    public static final String ONBOARDING_INFORMATION_REQUEST_NOT_FOUND = "Information request not found with id: %s";
    public static final String ONBOARDING_INFORMATION_REQUEST_NOT_OPEN = "Information request %s is not open";
    public static final String NEW_CASE_OWNER_MANDATORY = "newCaseOwnerUserId is mandatory";
    public static final String ASSIGNEE_USER_ID_MANDATORY = "assigneeUserId is mandatory";
    public static final String COMMENT_MESSAGE_MANDATORY = "message is mandatory";
    public static final String INFORMATION_RESPONSE_MANDATORY = "response is mandatory";

    // ---------- Onboarding documents ----------
    public static final String DOCUMENT_NOT_FOUND_BY_ID = "Document not found with id: %s";
    public static final String DOCUMENT_FILE_MANDATORY = "A file is required";
    public static final String DOCUMENT_TYPE_MANDATORY = "documentType is mandatory";
    public static final String DOCUMENT_EMPTY = "The file is empty";
    public static final String DOCUMENT_TOO_LARGE = "The file exceeds the maximum size of %s bytes";
    public static final String DOCUMENT_LIMIT_REACHED = "Application %s already has the maximum of %s documents";
    public static final String DOCUMENT_UPLOAD_NOT_ALLOWED =
            "Application %s does not accept uploads in its current status (%s); documents can be added while it is a draft, or under review while an information request is open";
    public static final String DOCUMENT_NOT_AVAILABLE = "Document %s is not available yet (status: %s)";
    public static final String DOCUMENT_NOT_DELETABLE = "Document %s can only be deleted while the application is a draft";
    public static final String DOCUMENT_NOT_CLEAN = "Document %s can only be verified or rejected once it is clean (current status: %s)";
    public static final String DOCUMENT_REJECTION_REASON_MANDATORY = "A reason is mandatory when rejecting a document";
    public static final String DOCUMENT_REQUIRED_MISSING = "Required document(s) missing or still being checked: %s";
    public static final String DOCUMENT_REQUIRED_NOT_VERIFIED = "Required document(s) not verified yet: %s";
    public static final String DOCUMENT_STORAGE_UNAVAILABLE = "Document storage is unavailable right now; please try again later";
    // Shown to the applicant as the reason a file was blocked by the automatic checks.
    public static final String DOCUMENT_BLOCKED_TYPE_NOT_ALLOWED =
            "The file type could not be recognised or is not allowed (only PDF, PNG and JPEG files are accepted)";
    public static final String DOCUMENT_BLOCKED_TYPE_MISMATCH = "The file content does not match the file type it was uploaded as";
    public static final String DOCUMENT_BLOCKED_PDF_UNREADABLE = "The PDF could not be read";
    public static final String DOCUMENT_BLOCKED_PDF_ENCRYPTED = "The PDF is encrypted or password protected";
    public static final String DOCUMENT_BLOCKED_PDF_TOO_MANY_PAGES = "The PDF has too many pages";
    public static final String DOCUMENT_BLOCKED_PDF_ACTIVE_CONTENT = "The PDF contains active content (scripts, launch actions or embedded files)";
    public static final String DOCUMENT_BLOCKED_IMAGE_UNREADABLE = "The image could not be read";
    public static final String DOCUMENT_BLOCKED_IMAGE_TOO_LARGE = "The image dimensions are too large";
    public static final String DOCUMENT_BLOCKED_PROCESSING_FAILED = "The file could not be checked after several attempts; please upload it again";
    public static final String DOCUMENT_BLOCKED_MALWARE = "The file failed the malware scan";

    // ---------- Platform access model ----------
    public static final String ROLE_SUPER_ADMIN_ONLY = "Only a super admin can grant or revoke role %s";
    public static final String ROLE_SELF_GRANT_FORBIDDEN = "You cannot grant a platform role to yourself";
    public static final String ROLE_TARGET_IS_AIRLINE_MEMBER = "User %s is an airline member and cannot hold a platform role";
    public static final String ROLE_CONFLICTS_WITH_HELD_ROLE = "Role %s cannot be combined with role %s held by user %s";
    public static final String ROLE_SCOPE_NOT_ASSIGNABLE = "Role %s has scope %s and cannot be granted directly to a user";
    public static final String MEMBERSHIP_CHECK_UNAVAILABLE = "Airline membership cannot be verified right now; please try again later";
    public static final String USER_ROLE_CHECK_UNAVAILABLE = "User roles cannot be verified right now; please try again later";

    // ---------- Bean validation: fields ----------
    public static final String FIRST_NAME_MANDATORY = "firstName is mandatory";
    public static final String LAST_NAME_MANDATORY = "lastName is mandatory";
    public static final String EMAIL_MANDATORY = "Email is mandatory";
    public static final String EMAIL_INVALID = "Email should be valid";
    public static final String NAME_MANDATORY = "name is mandatory";
    public static final String STATUS_MANDATORY = "status is mandatory";
    public static final String SCOPE_MANDATORY = "scope is mandatory";
    public static final String ROLE_SCOPE_INVALID = "scope must be PLATFORM, AIRLINE or BASELINE";
    public static final String ROLE_MANDATORY = "role is mandatory";
    public static final String PERMISSION_MANDATORY = "permission is mandatory";
    public static final String USER_MANDATORY = "user is mandatory";
    public static final String TOKEN_HASH_MANDATORY = "tokenHash is mandatory";
    public static final String EXPIRES_AT_MANDATORY = "expiresAt is mandatory";
    public static final String PASSWORD_MIN_LENGTH = "Password must be at least 8 characters";
}
