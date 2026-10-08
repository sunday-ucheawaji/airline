package com.sunday.common_lib.constants;

/**
 * Permission names payment-service checks. Airline-scoped permissions are checked against the caller's membership
 * of the payment's (denormalized) airlineId, through airline-core-service, same pattern as BookingPermissions.
 * PAYMENT_REFUND_EXECUTE is deliberately separate from BookingPermissions.TICKET_REFUND_APPROVE: that permission is
 * a booking-domain decision ("does this ticket qualify for a refund"), this one is payment-service's own gate on
 * an irreversible money-movement action, kept independently revocable. PAYMENT_SUPPORT_READ is a platform
 * permission (customer care / finance, read-only), mirroring BookingPermissions.BOOKING_SUPPORT_READ.
 */
public final class PaymentPermissions {

    private PaymentPermissions() {}

    /** Read an airline's payment records. RESERVATIONS_OFFICER, TICKETING_OFFICER, OWNER, ADMIN. */
    public static final String PAYMENT_READ = "PAYMENT_READ";

    /** Execute a refund at the payment provider for an airline's payment. REFUND_APPROVER, OWNER, ADMIN. */
    public static final String PAYMENT_REFUND_EXECUTE = "PAYMENT_REFUND_EXECUTE";

    /** Customer self-service: read own payment records. TRAVELLER. */
    public static final String PAYMENT_READ_OWN = "PAYMENT_READ_OWN";

    /** Customer care / finance read of any payment across airlines. Platform permission. */
    public static final String PAYMENT_SUPPORT_READ = "PAYMENT_SUPPORT_READ";
}
