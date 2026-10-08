package com.sunday.common_lib.constants;

/**
 * Permission names booking-service checks. Airline-scoped permissions are checked against the caller's membership of
 * the booking's airline (through airline-core-service) and are granted only to airline roles. The customer
 * permissions (_OWN) are granted to the TRAVELLER baseline role; the owner check itself is done by comparing the
 * caller with the booking's userId. BOOKING_SUPPORT_READ is a platform permission (customer care, read-only, masked).
 */
public final class BookingPermissions {

    private BookingPermissions() {}

    /** Read bookings of an airline. RESERVATIONS_OFFICER, TICKETING_OFFICER, OWNER, ADMIN. */
    public static final String BOOKING_READ = "BOOKING_READ";

    /** Create, change and cancel an airline's bookings. RESERVATIONS_OFFICER, OWNER, ADMIN. */
    public static final String BOOKING_MANAGE = "BOOKING_MANAGE";

    /** Read passenger records of an airline's bookings, without personal identifiers. */
    public static final String PASSENGER_READ = "PASSENGER_READ";

    /** Read passport number and date of birth of an airline's passengers. TICKETING_OFFICER, OWNER, ADMIN. */
    public static final String PASSENGER_PII_READ = "PASSENGER_PII_READ";

    /** Read tickets of an airline's bookings. */
    public static final String TICKET_READ = "TICKET_READ";

    /** Issue a ticket. TICKETING_OFFICER, OWNER, ADMIN. */
    public static final String TICKET_ISSUE = "TICKET_ISSUE";

    /** Void a ticket. TICKETING_OFFICER, OWNER, ADMIN. */
    public static final String TICKET_VOID = "TICKET_VOID";

    /** Approve a refund. REFUND_APPROVER, OWNER, ADMIN. The approver must not be the officer who requested it. */
    public static final String TICKET_REFUND_APPROVE = "TICKET_REFUND_APPROVE";

    /** Customer care read of any booking, passengers masked. Platform permission. */
    public static final String BOOKING_SUPPORT_READ = "BOOKING_SUPPORT_READ";

    /** Customer self-service: book a trip. TRAVELLER. */
    public static final String BOOKING_CREATE_OWN = "BOOKING_CREATE_OWN";

    /** Customer self-service: read own bookings. TRAVELLER. */
    public static final String BOOKING_READ_OWN = "BOOKING_READ_OWN";

    /** Customer self-service: cancel own booking, within fare rules. TRAVELLER. */
    public static final String BOOKING_CANCEL_OWN = "BOOKING_CANCEL_OWN";

    /** Customer self-service: read own passengers. TRAVELLER. */
    public static final String PASSENGER_READ_OWN = "PASSENGER_READ_OWN";

    /** Customer self-service: read own tickets. TRAVELLER. */
    public static final String TICKET_READ_OWN = "TICKET_READ_OWN";
}
