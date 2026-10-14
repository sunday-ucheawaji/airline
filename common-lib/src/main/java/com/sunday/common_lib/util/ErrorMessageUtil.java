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
    public static final String PASSWORD_TOO_LONG = "Password must be at most 72 bytes";
    public static final String CURRENT_PASSWORD_INCORRECT = "Current password is incorrect";
    public static final String PASSWORD_UNCHANGED = "The new password must differ from the current one";
    public static final String USER_STATUS_INVALID = "Status must be ACTIVE or SUSPENDED";
    public static final String USER_STATUS_SELF_CHANGE_FORBIDDEN = "You cannot change the status of your own account";
    public static final String USER_STATUS_SUPER_ADMIN_ONLY = "Only a super admin can change the status of a super admin";
    public static final String LAST_SUPER_ADMIN = "The last active super admin cannot be removed or suspended";
    public static final String ACCESS_REVOCATION_UNAVAILABLE = "Cannot revoke the user's access tokens right now; try again";

    // ---------- Password reset ----------
    public static final String INVALID_PASSWORD_RESET_TOKEN = "Invalid or expired password reset token";

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
    public static final String ROLE_NOT_FOUND_BY_NAME = "Role not found with name: %s";
    public static final String ROLE_ALREADY_EXISTS = "Role with name %s already exists";
    public static final String PERMISSION_NOT_FOUND_BY_ID = "Permission not found with id: %s";
    public static final String PERMISSION_ALREADY_EXISTS = "Permission with name %s already exists";
    public static final String PERMISSION_NAME_RESERVED_PREFIX = "Permission names cannot start with ROLE_ (reserved for roles in the access token)";
    public static final String PERMISSION_SCOPE_INVALID = "Permission scope must be PLATFORM or AIRLINE";
    public static final String PERMISSION_SCOPE_MISMATCH = "Permission %s is %s-scoped and cannot be granted to %s role %s";
    public static final String ROLE_BASELINE_NOT_CREATABLE = "BASELINE roles apply to every non-staff user and can only be created by a migration";
    public static final String ROLE_NOT_ACTIVE = "Role %s is not active and cannot be granted";
    public static final String PERMISSION_NOT_ASSIGNED_TO_ROLE = "Permission %s is not assigned to role %s";
    public static final String ROLE_NOT_PLATFORM_SCOPED =
            "Role %s is airline-scoped and cannot be assigned directly to a user; airline-scoped roles are granted through airline membership";
    public static final String PLATFORM_ROLE_NOT_ASSIGNED_TO_USER = "Role %s is not assigned to user %s";

    // ---------- Airline ----------
    public static final String AIRLINE_NOT_FOUND_BY_ID = "Airline not found with id: %s";
    public static final String NO_ACTIVE_MEMBERSHIP_FOR_AIRLINE = "You do not have an active membership in airline %s";
    public static final String NO_ACTIVE_MEMBERSHIP_IN_ACTIVE_AIRLINE = "You do not have an active membership in airline %s, or the airline is not active";
    public static final String NO_PERMISSION_FOR_AIRLINE = "You do not have permission %s for airline %s";
    public static final String NO_PERMISSION_FOR_AIRLINES = "You do not have permission %s for airline(s): %s";
    public static final String NO_PERMISSION_FOR_AIRCRAFT = "You do not have permission to view aircraft %s";
    public static final String AIRLINE_NOT_ACTIVE = "Airline %s is %s and cannot be modified";
    public static final String AIRLINE_IATA_CODE_ALREADY_EXISTS = "IATA code %s is already in use by another airline";
    public static final String AIRLINE_ICAO_CODE_ALREADY_EXISTS = "ICAO code %s is already in use by another airline";
    public static final String AIRLINE_STATUS_UNCHANGED = "Airline %s is already %s";
    public static final String CONCURRENT_MODIFICATION = "The record was changed by someone else at the same time; reload it and try again";
    public static final String AIRLINE_OF_FLIGHT_UNAVAILABLE = "The airline operating flight %s could not be resolved; try again";
    public static final String AIRLINE_NOT_ACCEPTING_BOOKINGS = "Airline %s is %s and is not accepting new bookings";
    public static final String AIRLINE_IDENTITY_CHANGE_NOT_FOUND = "Identity change request %s not found";
    public static final String AIRLINE_IDENTITY_CHANGE_NOTHING_CHANGED = "The request does not change any identity field";
    public static final String AIRLINE_IDENTITY_CHANGE_ALREADY_PENDING = "Airline %s already has a pending identity change request";
    public static final String AIRLINE_IDENTITY_CHANGE_NOT_PENDING = "Identity change request %s is %s, not PENDING";
    public static final String AIRLINE_IDENTITY_CHANGE_CANCEL_NOT_REQUESTER = "Only the member who made identity change request %s can cancel it";
    public static final String AIRLINE_IDENTITY_CHANGE_REVIEWER_IS_REQUESTER = "You cannot review an identity change request you made";
    public static final String AIRLINE_IDENTITY_CHANGE_REJECTION_REASON_MANDATORY = "A reason is required to reject an identity change request";
    public static final String USER_ACCESS_LOCK_BUSY = "Another change to the access of user %s is in progress; try again";
    public static final String USER_ACCESS_LOCK_UNAVAILABLE = "Cannot lock the access of user %s right now";
    public static final String DOCUMENT_SCANNER_DEFINITIONS_STALE = "Virus definitions are out of date (%s); documents will be scanned once they are updated";
    public static final String DOCUMENT_SCANNER_DEFINITIONS_UNKNOWN = "Could not determine the age of the virus definitions";
    public static final String AIRLINE_CLOSED_STATUS_LOCKED = "Airline %s has been closed and its status can no longer be changed";

    // ---------- Aircraft ----------
    public static final String AIRCRAFT_NOT_FOUND_BY_ID = "Aircraft not found with id: %s";
    public static final String AIRCRAFT_CODE_ALREADY_EXISTS = "Aircraft with code %s already exists";
    public static final String AIRCRAFT_SEATS_EXCEED_CAPACITY = "Total specified seats exceed aircraft seating capacity";
    public static final String AIRCRAFT_INVALID_YEAR_OF_MANUFACTURE = "Invalid year of manufacture";

    // ---------- Flight ops: Flight ----------
    public static final String FLIGHT_NUMBER_MANDATORY = "Flight number is required";
    public static final String DEPARTURE_AIRPORT_ID_MANDATORY = "Departure airport ID is required";
    public static final String ARRIVAL_AIRPORT_ID_MANDATORY = "Arrival airport ID is required";
    public static final String FLIGHT_NUMBER_ALREADY_EXISTS = "Flight with number '%s' already exists";
    public static final String FLIGHT_NOT_FOUND_BY_ID = "Flight not found with id: %s";
    public static final String FLIGHT_NOT_FOUND_BY_NUMBER = "Flight not found with number: %s";

    // ---------- Flight ops: FlightInstance ----------
    public static final String FLIGHT_ID_MANDATORY = "Flight ID is required";
    public static final String AIRCRAFT_ID_MANDATORY = "Aircraft ID is required";
    public static final String DEPARTURE_DATETIME_MANDATORY = "Departure date-time is required";
    public static final String ARRIVAL_DATETIME_MANDATORY = "Arrival date-time is required";
    public static final String TOTAL_SEATS_MANDATORY = "Total seats is required";
    public static final String FLIGHT_INSTANCE_NOT_FOUND_BY_ID = "Flight instance not found with id: %s";
    public static final String SEAT_SERVICE_UNAVAILABLE = "Seat service unavailable";
    public static final String AIRCRAFT_REASSIGN_CAPACITY_EXCEEDED =
            "Cannot reassign: %d %s seat(s) already sold or held, but the new aircraft has %s";

    // ---------- Flight ops: FlightSchedule ----------
    public static final String DEPARTURE_TIME_MANDATORY = "Departure time is required";
    public static final String ARRIVAL_TIME_MANDATORY = "Arrival time is required";
    public static final String SCHEDULE_START_DATE_MANDATORY = "Start date is required";
    public static final String SCHEDULE_END_DATE_MANDATORY = "End date is required";
    public static final String SCHEDULE_END_DATE_BEFORE_START_DATE = "End date must be after start date";
    public static final String FLIGHT_SCHEDULE_NOT_FOUND_BY_ID = "Flight schedule not found with id: %s";
    public static final String SCHEDULE_OPERATING_DAYS_MANDATORY = "Operating days are required unless recurrenceType is DAILY";

    // ---------- Flight ops: search ----------
    public static final String SEARCH_DEPARTURE_DATE_MANDATORY = "Departure date is required";
    public static final String SEARCH_PASSENGERS_MANDATORY = "Number of passengers is required";
    public static final String SEARCH_PASSENGERS_MIN = "At least 1 passenger is required";
    public static final String SEARCH_CABIN_CLASS_MANDATORY = "Cabin class is required";

    // ---------- Flight ops: cross-service integration ----------
    public static final String AIRLINE_SERVICE_ERROR = "Airline service error: %s";
    public static final String AIRLINE_SERVICE_UNAVAILABLE = "Airline service unavailable";
    public static final String FLIGHT_SERVICE_ERROR = "Flight service error: %s";
    public static final String FLIGHT_SERVICE_UNAVAILABLE = "Flight service unavailable";
    public static final String AIRCRAFT_SERVICE_ERROR = "Aircraft service error: %s";

    // ---------- Pricing ----------
    public static final String FARE_NOT_FOUND_BY_ID = "Fare not found with id: %s";
    public static final String FARE_ALREADY_EXISTS = "Fare '%s' already exists for this flight and cabin class";
    public static final String FARE_RULES_NOT_FOUND_BY_ID = "Fare rules not found with id: %s";
    public static final String FARE_RULES_NOT_FOUND_FOR_FARE = "Fare rules not found for fare id: %s";
    public static final String FARE_RULES_ALREADY_EXIST = "Fare rules already exist for fare id: %s";
    public static final String BAGGAGE_POLICY_NOT_FOUND_BY_ID = "Baggage policy not found with id: %s";
    public static final String BAGGAGE_POLICY_NOT_FOUND_FOR_FARE = "Baggage policy not found for fare id: %s";
    public static final String BAGGAGE_POLICY_ALREADY_EXISTS = "Baggage policy already exists for fare id: %s";
    public static final String FARE_SALES_WINDOW_INVALID = "Sales start must be on or before sales end";
    public static final String FARE_SERVICE_ERROR = "Fare service error: %s";
    public static final String FARE_SERVICE_UNAVAILABLE = "Fare service unavailable";
    public static final String FARE_UNAVAILABLE_FOR_DEPARTURE = "The selected fare is not available for this departure";
    public static final String FARE_TRAVEL_WINDOW_INVALID = "Travel start date must be on or before travel end date";
    public static final String FARE_CURRENCY_INVALID = "Currency '%s' is not a valid ISO-4217 code";
    public static final String FARE_RBD_INVALID = "RBD code must be a letter A to Z";
    public static final String FARE_NOT_PUBLISHED = "Fare is not published";
    public static final String FARE_OUTSIDE_SALES_WINDOW = "Fare is not on sale at this time";
    public static final String FARE_OUTSIDE_TRAVEL_WINDOW = "Fare does not apply to this departure date";
    public static final String FARE_ADVANCE_PURCHASE_NOT_MET = "Fare must be bought at least %s days before departure";
    public static final String FARE_INVALID_STATUS_TRANSITION = "Fare cannot move from %s to %s";
    public static final String FARE_FLIGHT_MISMATCH = "Fare %s does not belong to flight %s";
    public static final String FARE_CABIN_CLASS_MISMATCH = "Fare %s is not for cabin class %s";
    public static final String FARE_PASSENGER_TYPE_MISMATCH = "Fare %s is not for passenger type %s";
    public static final String FARE_DELETE_NOT_DRAFT = "Only a draft fare can be deleted; withdraw a fare that has been published";
    public static final String FARE_LOCKED_WHILE_PUBLISHED = "Fare %s is published; withdraw it before changing it, its rules or its baggage policy";
    public static final String DATA_INTEGRITY_CONFLICT = "The request conflicts with existing data";
    public static final String BOOKING_FARE_CURRENCY_MISMATCH = "The %s fare is priced in %s, but the booking is in %s";
    public static final String BOOKING_CURRENCY_NOT_ACCEPTED = "Payments in %s are not accepted";
    public static final String TRIP_ONE_WAY_NOT_A_TRIP = "A one-way booking is made through /api/bookings";
    public static final String TRIP_ROUND_TRIP_NEEDS_TWO_FLIGHTS = "A round trip needs exactly 2 flights, got %s";
    public static final String TRIP_MULTI_CITY_FLIGHT_COUNT = "A multi-city trip needs 2 to %s flights, got %s";
    public static final String TRIP_SEAT_PER_PASSENGER = "Flight %s needs one seat entry per passenger (%s), got %s";
    public static final String TRIP_FLIGHTS_OUT_OF_ORDER = "Flight %s must depart after the flight before it";
    public static final String TRIP_ONE_AIRLINE = "Every flight of a trip must be with the same airline";
    public static final String TRIP_NOT_A_ROUND_TRIP = "The return flight must go back from where the outbound flight lands, to where it left";
    public static final String TRIP_CURRENCY_MISMATCH = "Every fare of a trip must be in the same currency";
    public static final String TRIP_NOT_FOUND_BY_ID = "Trip not found with id: %s";
    public static final String TRIP_SEGMENTS_MANDATORY = "A trip needs its flights";
    public static final String TRIP_PASSENGERS_MANDATORY = "A trip needs at least one passenger";
    public static final String EXTRAS_BOOKING_NOT_CONFIRMED = "Extras can only be added to a confirmed booking; booking %s is %s";
    public static final String EXTRAS_AFTER_DEPARTURE = "Booking %s has already departed";
    public static final String EXTRAS_EMPTY = "Choose at least one ancillary or meal";
    public static final String PAYABLE_NOT_FOUND = "Nothing to pay with id: %s";
    public static final String BOOKING_NEEDS_ADULT = "A booking needs at least one adult passenger";
    public static final String BOOKING_TOO_MANY_INFANTS = "Each lap infant must travel on an adult's lap: %s lap infants for %s adults";
    public static final String PASSENGER_SEAT_REQUIRED = "Passenger %s %s needs a seat; only an infant can travel on an adult's lap";
    public static final String LAP_INFANT_NEEDS_PASSENGER_ID = "A passenger without a seat must be identified by their passenger id";
    public static final String PASSENGER_NOT_IN_BOOKING = "Passenger %s is not on booking %s";
    public static final String BOOKING_NOT_CHANGEABLE_STATUS = "Booking %s is %s; only a confirmed booking can be changed";
    public static final String BOOKING_FARE_NOT_CHANGEABLE = "The fare on booking %s does not allow changes";
    public static final String BOOKING_CHANGE_DEADLINE_PASSED = "Booking %s can no longer be changed: changes close %s hours before departure";
    public static final String BOOKING_CHANGE_PENDING = "Booking %s already has a change waiting for payment";
    public static final String BOOKING_CHANGE_OTHER_AIRLINE = "A change must stay with the airline of booking %s";
    public static final String BOOKING_EXCHANGED = "Exchanged for booking %s";
    public static final String BOOKING_CHANGE_FLIGHT_INSTANCE_MANDATORY = "The new flight instance is required";
    public static final String BOOKING_CHANGE_FARE_MANDATORY = "The new fare is required";
    public static final String SEAT_ASSIGNMENT_PASSENGER_MANDATORY = "Passenger id is required for each seat";
    public static final String SEAT_ASSIGNMENT_SEAT_MANDATORY = "Seat instance id is required for each seat";
    public static final String PASSENGER_TYPE_CHANGE_NOT_ALLOWED = "A date of birth change cannot move a passenger from %s to %s; cancel and rebook instead";
    public static final String FARE_SALES_WINDOW_ENDED = "Fare sales window has already ended";
    public static final String FARE_TRAVEL_WINDOW_ENDED = "Fare travel window has already ended";
    public static final String FARE_TERMS_MISSING = "Fare needs both fare rules and a baggage policy before it can be published";
    public static final String FARE_SEARCH_TOO_LARGE = "A search can cover at most %s flight departures";
    public static final String FARE_DUPLICATE_IN_REQUEST = "The same fare appears more than once in this request";
    public static final String BAGGAGE_POLICY_DUPLICATE_IN_REQUEST = "A baggage policy for this fare appears more than once in this request";
    public static final String BULK_TOO_LARGE = "A bulk request can contain at most %s rows";

    // ---------- Airline membership (invitations) ----------
    public static final String MEMBER_INVITE_EMAIL_MANDATORY = "A valid email is required";
    public static final String MEMBER_INVITE_ROLE_MANDATORY = "roleId is required";
    public static final String MEMBER_STATUS_MANDATORY = "status is required";
    public static final String MEMBER_INVITE_TARGET_ROLE_INVALID = "Role %s cannot be invited to an airline; OWNER is never invitable";
    public static final String MEMBER_INVITE_USER_NOT_FOUND = "No user found with email %s";
    public static final String MEMBER_INVITE_TARGET_IS_PLATFORM_STAFF = "User %s holds a platform role and cannot join an airline";
    public static final String MEMBER_ALREADY_ACTIVE = "User %s is already a member of airline %s";
    public static final String MEMBER_ALREADY_INVITED = "User %s already has a pending invitation to airline %s";
    public static final String MEMBERSHIP_NOT_FOUND = "Membership %s not found for airline %s";
    public static final String MEMBERSHIP_NOT_PENDING = "Membership %s is not a pending invitation";
    public static final String MEMBERSHIP_INVITATION_EXPIRED = "This invitation has expired";
    public static final String MEMBERSHIP_ACCEPT_NOT_INVITEE = "Only the invited user can accept this invitation";
    public static final String MEMBERSHIP_NOT_ACTIVE_OR_SUSPENDED = "Membership %s is not an active or suspended member";
    public static final String MEMBER_ROLE_UPDATE_TARGET_INVALID = "Role %s cannot be assigned to an existing member; OWNER is never invitable";
    public static final String MEMBER_STATUS_UPDATE_INVALID = "status must be ACTIVE or SUSPENDED";
    public static final String MEMBER_CANNOT_REMOVE_LAST_OWNER = "Cannot remove the last OWNER of airline %s";
    public static final String MEMBER_ROLE_UPDATE_OWNER_LOCKED = "The OWNER's role cannot be changed here";
    public static final String ROLE_LOOKUP_UNAVAILABLE = "Role %s cannot be verified right now; please try again later";

    // ---------- Ancillary ----------
    public static final String AIRLINE_ID_REQUIRED = "airlineId is required";
    public static final String ANCILLARY_NOT_FOUND_BY_ID = "Ancillary not found with id: %s";
    public static final String ANCILLARY_NOT_AVAILABLE = "Ancillary %s is not available";
    public static final String ANCILLARY_MAX_QUANTITY_EXCEEDED = "Ancillary %s: at most %s per passenger, so %s for this booking; requested %s";
    public static final String FLIGHT_CABIN_ANCILLARY_WRONG_CABIN = "FlightCabinAncillary %s is for cabin %s, not %s";
    public static final String MEALS_EXCEED_PASSENGERS = "%s meals requested for %s passengers; at most one meal per passenger";
    public static final String PASSENGER_COUNT_INVALID = "The passenger count must be at least 1";
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
    public static final String FLIGHT_CABIN_ANCILLARY_WRONG_FLIGHT = "FlightCabinAncillary %s does not belong to flight %s";
    public static final String FLIGHT_MEAL_WRONG_FLIGHT = "FlightMeal %s does not belong to flight %s";
    public static final String FLIGHT_MEAL_ALREADY_ASSIGNED_TO_FLIGHT = "Meal %s is already assigned to flight %s";
    public static final String FLIGHT_MEAL_DUPLICATE_IN_REQUEST = "Meal %s for flight %s is duplicated in this request";
    public static final String INSURANCE_COVERAGE_NOT_FOUND_BY_ID = "Insurance coverage not found with id: %s";
    public static final String MEAL_CODE_DUPLICATE_IN_REQUEST = "Meal code %s for airline %s is duplicated in this request";
    public static final String FLIGHT_CABIN_ANCILLARY_ALREADY_ASSIGNED = "Ancillary %s is already assigned to flight %s cabin %s";
    public static final String FLIGHT_CABIN_ANCILLARY_DUPLICATE_IN_REQUEST = "Ancillary %s for flight %s cabin %s is duplicated in this request";
    public static final String MAX_QUANTITY_NEGATIVE = "Max quantity must be zero or positive";
    public static final String DISPLAY_ORDER_NEGATIVE = "Display order cannot be negative";

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

    // ---------- Seat service ----------
    public static final String CABIN_CLASS_NOT_FOUND_BY_ID = "Cabin class not found with id: %s";
    public static final String CABIN_CLASS_CODE_ALREADY_EXISTS_FOR_AIRCRAFT = "Cabin class with code '%s' already exists for this aircraft";
    public static final String CABIN_CLASS_CODE_DUPLICATE_IN_REQUEST = "Cabin class code '%s' appears more than once in this request for the same aircraft";
    public static final String SEAT_MAP_NOT_FOUND_BY_ID = "Seat map not found with id: %s";
    public static final String SEAT_MAP_NOT_FOUND_BY_CABIN_CLASS_ID = "Seat map not found for cabin class id: %s";
    public static final String SEAT_MAP_NAME_ALREADY_EXISTS = "Seat map with name '%s' already exists for this airline and cabin class";
    public static final String SEAT_NOT_FOUND_BY_ID = "Seat not found with id: %s";
    public static final String SEAT_INSTANCE_NOT_FOUND_BY_ID = "Seat instance not found with id: %s";
    public static final String FLIGHT_INSTANCE_CABIN_NOT_FOUND_BY_ID = "Flight instance cabin not found with id: %s";
    public static final String SEAT_MAP_CABIN_CLASS_ALREADY_HAS_SEAT_MAP = "Cabin class %s already has a seat map; only one is allowed per cabin class";
    public static final String SEAT_MAP_CABIN_CLASS_DUPLICATE_IN_REQUEST = "Cabin class %s appears more than once in this request; only one seat map is allowed per cabin class";
    public static final String SEAT_INSTANCE_ILLEGAL_STATUS_TRANSITION = "Cannot transition seat instance %s from %s to %s";
    public static final String SEAT_INSTANCE_DUPLICATE_FOR_SEAT = "A seat instance already exists for seat %s on this flight instance";
    public static final String SEAT_INSTANCE_WRONG_FLIGHT_INSTANCE = "Seat instance %s does not belong to flight instance %s";
    public static final String SEAT_INSTANCE_WRONG_CABIN_CLASS = "Seat instance %s is not in cabin class %s";
    public static final String SEAT_INSTANCE_NOT_AVAILABLE_FOR_HOLD = "Seat instance %s is not available";
    public static final String SEAT_STRUCTURE_IN_USE = "%s %s is used by flight inventory and cannot be deleted";
    public static final String SEAT_MAP_LAYOUT_LOCKED = "Seat map %s already has flight inventory; its rows and seats per row can no longer change";
    public static final String CABIN_CLASS_NOT_FOUND_FOR_AIRCRAFT = "Aircraft %s has no %s cabin";
    public static final String CABIN_CLASS_AIRCRAFT_IMMUTABLE = "A cabin class cannot be moved to another aircraft";
    public static final String SEAT_CABIN_CLASS_OTHER_AIRCRAFT = "Cabin class %s belongs to a different aircraft than seat map %s";
    public static final String FLIGHT_INSTANCE_CABIN_DUPLICATE = "Flight instance %s already has a %s cabin";
    public static final String FLIGHT_INSTANCE_NOT_FOUND_BY_ID_FOR_SEATS = "Flight instance %s not found";
    public static final String FLIGHT_INSTANCE_AIRCRAFT_MISMATCH = "Flight instance %s is operated by aircraft %s, not by the aircraft of cabin class %s";
    public static final String FLIGHT_OPS_SERVICE_UNAVAILABLE = "Flight-ops service unavailable";
    public static final String FLIGHT_INSTANCE_CABIN_CLASS_IMMUTABLE = "A flight-instance cabin cannot switch cabin class; reassign the flight instance's aircraft instead";
    public static final String NO_SEATS_IN_SEAT_MAP = "No seats found in the seat map for cabin class id %s";
    public static final String FLIGHT_INSTANCE_CABIN_NOT_FOUND_FOR_FLIGHT_AND_CABIN_CLASS =
            "Flight instance cabin not found for flight instance id %s and cabin class id %s";

    // Seat service: request validation (Bean Validation message attributes)
    public static final String FLIGHT_INSTANCE_ID_MANDATORY = "Flight instance ID is required";
    public static final String CABIN_CLASS_NAME_MANDATORY = "Name is required";
    public static final String SEAT_MAP_NAME_MANDATORY = "Seat map name is required";
    public static final String SEAT_MAP_ID_MANDATORY = "Seat map ID is required";
    public static final String SEAT_MAP_TOTAL_ROWS_MANDATORY = "Total rows is required";
    public static final String SEAT_MAP_LEFT_SEATS_PER_ROW_MANDATORY = "Left seats per row is required";
    public static final String SEAT_MAP_RIGHT_SEATS_PER_ROW_MANDATORY = "Right seats per row is required";
    public static final String SEAT_NUMBER_MANDATORY = "Seat number is required";
    public static final String SEAT_ROW_MANDATORY = "Seat row is required";
    public static final String SEAT_TYPE_MANDATORY = "Seat type is required";
    public static final String SEAT_INSTANCE_IDS_MANDATORY = "seatInstanceIds is required";
    public static final String SEAT_INSTANCE_IDS_MAX_EXCEEDED = "A maximum of 50 seat instance ids can be priced per request";

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

    // ---------- Booking ----------
    public static final String FARE_ID_MANDATORY = "Fare ID is required";
    public static final String AT_LEAST_ONE_PASSENGER_REQUIRED = "At least one passenger is required";
    public static final String TRIP_TYPE_MANDATORY = "Trip type is required";
    public static final String ONLY_ONE_WAY_BOOKINGS_SUPPORTED = "Round-trip and multi-city bookings are made through /api/bookings/trips";
    public static final String FLIGHT_ID_DOES_NOT_MATCH_INSTANCE = "Flight %s does not match flight instance %s";
    public static final String BOOKING_NOT_FOUND_BY_ID = "Booking not found with ID: %s";
    public static final String BOOKING_NOT_FOUND_BY_REFERENCE = "Booking not found with reference: %s";
    public static final String BOOKING_NOT_MODIFIABLE = "Booking %s can no longer be changed (status %s)";
    public static final String BOOKING_NOT_PAYABLE = "Booking %s is not awaiting payment (status %s)";
    public static final String BOOKING_AMOUNT_MISMATCH = "Payment amount %s does not match booking total %s";
    public static final String BOOKING_SERVICE_UNAVAILABLE = "Booking service unavailable";
    public static final String BOOKING_SERVICE_ERROR = "Booking service error: %s";
    public static final String BOOKING_PAYMENT_NOT_OWNED = "You can only pay for your own bookings";
    public static final String IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST =
            "Idempotency-Key %s was already used with a different request body";
    public static final String IDEMPOTENCY_REQUEST_IN_PROGRESS =
            "A request with Idempotency-Key %s is already in progress";
    public static final String BOOKING_NOT_CANCELLABLE = "Booking %s cannot be cancelled (status %s)";
    public static final String BOOKING_HAS_USED_TICKETS = "Booking %s has a used ticket and can no longer be cancelled";
    public static final String BOOKING_UPDATE_DETAILS_ONLY =
            "Only passenger details and contact information can be changed; cancel and rebook to change the flight, fare, cabin, seats or extras";
    public static final String DUPLICATE_SEAT_IN_BOOKING = "Seat instance %s is assigned to more than one passenger";
    public static final String BOOKING_CANCELLED_BY_REQUEST = "Cancelled by request";
    public static final String BOOKING_NO_PAYMENT_TO_REFUND = "Booking %s is confirmed but has no payment to refund";
    public static final String BOOKING_CANCELLED_PAYMENT_TIMEOUT = "The booking was not paid in time and has been cancelled";
    public static final String BOOKING_CANCELLED_PAYMENT_FAILED = "Payment failed: %s";
    public static final String BOOKING_CANCELLED_AMOUNT_MISMATCH = "The amount paid did not match the booking total; the booking was cancelled and the payment refunded";

    // ---------- Passenger ----------
    public static final String PASSENGER_FIRST_NAME_MANDATORY = "First name is required";
    public static final String PASSENGER_FIRST_NAME_SIZE = "First name must be between 2 and 50 characters";
    public static final String PASSENGER_LAST_NAME_MANDATORY = "Last name is required";
    public static final String PASSENGER_LAST_NAME_SIZE = "Last name must be between 2 and 50 characters";
    public static final String PASSENGER_EMAIL_MANDATORY = "Email is required";
    public static final String PASSENGER_PHONE_MANDATORY = "Phone number is required";
    public static final String PASSENGER_PHONE_FORMAT_INVALID = "Phone number should be in international format";
    public static final String PASSENGER_DOB_MANDATORY = "Date of birth is required";
    public static final String PASSENGER_DOB_PAST = "Date of birth must be in the past";
    public static final String PASSENGER_GENDER_MANDATORY = "Gender is required";
    public static final String PASSENGER_SEAT_INSTANCE_ID_MANDATORY = "Seat instance ID is required";
    public static final String PASSENGER_NOT_FOUND_BY_ID = "Passenger not found with id: %s";

    // ---------- Ticket ----------
    public static final String TICKET_NOT_FOUND_BY_NUMBER = "Ticket not found with number: %s";
    public static final String TICKET_NOT_FOUND_BY_ID = "Ticket not found with id: %s";
    public static final String TICKET_USED_CANNOT_CANCEL = "Cannot cancel a ticket that has already been used";
    public static final String TICKET_REFUNDED_CANNOT_CANCEL = "Cannot cancel a ticket that has already been refunded";
    public static final String TICKET_CANCELLED_CANNOT_USE = "Cannot use a cancelled ticket";
    public static final String TICKET_REFUNDED_CANNOT_USE = "Cannot use a refunded ticket";
    public static final String TICKET_USED_CANNOT_REFUND = "Cannot refund a ticket that has already been used";

    // ---------- Payment ----------
    public static final String PAYMENT_USER_ID_MANDATORY = "User ID is mandatory";
    public static final String PAYMENT_BOOKING_ID_MANDATORY = "bookingId is required";
    public static final String PAYMENT_GATEWAY_MANDATORY = "Payment gateway is mandatory";
    public static final String PAYMENT_AMOUNT_MANDATORY = "Amount is mandatory";
    public static final String PAYMENT_AMOUNT_POSITIVE = "Amount must be positive";
    public static final String PAYMENT_CURRENCY_MANDATORY = "Currency is mandatory";
    public static final String PAYMENT_CURRENCY_INVALID = "Currency must be a three-letter ISO 4217 code";
    public static final String PAYMENT_CURRENCY_MISMATCH = "Payment currency %s does not match booking currency %s";
    public static final String PAYMENT_CURRENCY_NOT_SUPPORTED = "Currency %s is not supported";
    public static final String PAYMENT_REFUND_AMOUNT_INVALID = "Refund amount %s must be positive and at most the %s still refundable";
    public static final String PAYMENT_ALREADY_COMPLETED = "Payment already completed for this booking";
    public static final String PAYMENT_NOT_FOUND_BY_ID = "Payment not found with ID: %s";
    public static final String PAYMENT_NOT_FOUND_FOR_BOOKING = "No payment found for booking: %s";

    // ---------- Notification ----------
    public static final String NOTIFICATION_NOT_FOUND_BY_ID = "Notification not found with ID: %s";
    public static final String NOTIFICATION_CHANNEL_NOT_SUPPORTED = "No provider configured for notification channel %s";
    public static final String NOTIFICATION_DELIVERY_FAILED = "Failed to deliver %s notification: %s";
    public static final String PAYMENT_INITIATION_FAILED = "Failed to initiate payment: %s";
    public static final String PAYMENT_VERIFICATION_FAILED = "Payment verification failed";
    public static final String PAYMENT_CAPTURED_AMOUNT_MISMATCH = "Captured amount does not match the payment amount";
    public static final String PAYMENT_SERVICE_UNAVAILABLE = "Payment service is temporarily unavailable. Please retry.";
    public static final String RAZORPAY_WEBHOOK_PROCESSING_FAILED = "Failed to process Razorpay webhook: %s";
    public static final String RAZORPAY_NOT_CONFIGURED = "Razorpay is not configured; set its API key and secret";
    public static final String RAZORPAY_PAYMENT_LINK_FAILED = "Failed to create payment link: %s";
    public static final String RAZORPAY_FETCH_PAYMENT_FAILED = "Failed to fetch payment details: %s";
    public static final String RAZORPAY_REFUND_FAILED = "Failed to refund Razorpay payment: %s";
    public static final String STRIPE_NOT_CONFIGURED = "Stripe is not configured; set its API key";
    public static final String STRIPE_CHECKOUT_FAILED = "Failed to create Stripe checkout session: %s";
    public static final String STRIPE_FETCH_PAYMENT_FAILED = "Failed to fetch Stripe payment intent: %s";
    public static final String STRIPE_REFUND_FAILED = "Failed to refund Stripe payment: %s";
    public static final String STRIPE_WEBHOOK_PROCESSING_FAILED = "Failed to process Stripe webhook: %s";
    public static final String PAYSTACK_NOT_CONFIGURED = "Paystack is not configured; set its secret key";
    public static final String PAYSTACK_CHECKOUT_FAILED = "Failed to initialize Paystack transaction: %s";
    public static final String PAYSTACK_FETCH_PAYMENT_FAILED = "Failed to verify Paystack transaction: %s";
    public static final String PAYSTACK_REFUND_FAILED = "Failed to refund Paystack transaction: %s";
    public static final String PAYSTACK_WEBHOOK_PROCESSING_FAILED = "Failed to process Paystack webhook: %s";
    public static final String WEBHOOK_SIGNATURE_INVALID = "Webhook signature verification failed";
    public static final String PAYMENT_GATEWAY_NOT_SUPPORTED = "No payment provider is configured for gateway %s";
    public static final String PAYMENT_NOT_REFUNDABLE = "Payment %s is not refundable (status %s)";
    public static final String BOOKING_CANCELLED_NO_SEATS = "The seats chosen were no longer available and the cabin is full; the booking was cancelled and the payment refunded";
    public static final String PAYMENT_SERVICE_REFUND_UNAVAILABLE = "Payment service could not refund payment %s";
    public static final String PAYMENT_REFUND_FAILED = "Failed to refund payment %s: %s";

    // ---------- Location: City ----------
    public static final String CITY_NAME_MANDATORY = "City name is required";
    public static final String CITY_CODE_MANDATORY = "City code is required";
    public static final String COUNTRY_CODE_MANDATORY = "Country code is required";
    public static final String COUNTRY_NAME_MANDATORY = "Country name is required";
    public static final String CITY_CODE_ALREADY_EXISTS = "City with code %s already exists";
    public static final String CITY_NOT_FOUND_BY_ID = "City not found with id: %s";
    public static final String CITY_STILL_HAS_AIRPORTS = "City %s still has airports; remove or reassign them first";
    public static final String CITY_CODE_FORMAT_INVALID = "Invalid city code format. Must be 2-10 alphanumeric characters.";
    public static final String CITY_COUNTRY_CODE_FORMAT_INVALID = "Country code must be 2-5 uppercase letters";
    public static final String TIMEZONE_MANDATORY = "Time zone is required";
    public static final String TIMEZONE_INVALID = "Invalid timezone: %s";
    public static final String CITY_DELETED_SUCCESSFULLY = "City deleted successfully";
    public static final String CITY_CODE_DUPLICATE_IN_REQUEST = "City code %s is duplicated in this request";
    public static final String CITY_REASSIGN_SAME_CITY = "Source and target city must be different";
    public static final String CITY_AIRPORTS_REASSIGNED = "Reassigned %s airport(s) from %s to %s";

    // ---------- Location: Airport ----------
    public static final String AIRPORT_IATA_CODE_MANDATORY = "IATA code is mandatory";
    public static final String AIRPORT_IATA_CODE_LENGTH = "IATA code must be exactly 3 characters";
    public static final String AIRPORT_NAME_MANDATORY = "Airport name is mandatory";
    public static final String AIRPORT_CITY_ID_MANDATORY = "City ID is mandatory";
    public static final String AIRPORT_IATA_CODE_ALREADY_EXISTS = "Airport with IATA code %s already exists";
    public static final String AIRPORT_IATA_CODE_TAKEN = "IATA code %s is already taken";
    public static final String AIRPORT_IATA_CODE_DUPLICATE_IN_REQUEST = "IATA code %s is duplicated in this request";
    public static final String AIRPORT_NOT_FOUND_BY_ID = "Airport not found with id: %s";
    public static final String AIRPORT_DELETED_SUCCESSFULLY = "Airport deleted successfully";
    public static final String AIRPORT_LATITUDE_OUT_OF_RANGE = "Latitude must be between -90 and 90";
    public static final String AIRPORT_LONGITUDE_OUT_OF_RANGE = "Longitude must be between -180 and 180";

    // ---------- Location: request validation (Bean Validation message attributes) ----------
    public static final String SORT_BY_FIELD_INVALID = "sortBy must be one of %s";
    public static final String SORT_DIRECTION_INVALID = "sortDirection must be 'asc' or 'desc'";

    // ---------- Ancillary: request validation (Bean Validation message attributes) ----------
    public static final String PRICE_MANDATORY = "Price is required";
    public static final String PRICE_MUST_BE_ZERO_OR_POSITIVE = "Price must be zero or positive";
    public static final String FLIGHT_MEAL_PRICE_NEGATIVE = "Price cannot be negative";
    public static final String COVERAGE_AMOUNT_MANDATORY = "Coverage amount is required";
    public static final String COVERAGE_AMOUNT_MUST_BE_ZERO_OR_POSITIVE = "Coverage amount must be zero or positive";
    public static final String COVERAGE_AMOUNT_TOO_LARGE = "Coverage amount must have at most 10 integer digits and 2 decimal places";
}
