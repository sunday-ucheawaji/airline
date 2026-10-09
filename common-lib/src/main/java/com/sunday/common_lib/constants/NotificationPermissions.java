package com.sunday.common_lib.constants;

/**
 * Permission names notification-service checks. Unlike booking/payment, a notification has no airline
 * owner of its own (it's a side effect of a booking/payment event, addressed to the customer) — both
 * permissions here are platform-tier only, mirroring BookingPermissions.BOOKING_SUPPORT_READ /
 * PaymentPermissions.PAYMENT_SUPPORT_READ rather than an airline-scoped read/manage pair.
 */
public final class NotificationPermissions {

    private NotificationPermissions() {}

    /** Read a booking's notification delivery history (what was sent, to whom, status). Platform permission. */
    public static final String NOTIFICATION_SUPPORT_READ = "NOTIFICATION_SUPPORT_READ";

    /** Force a resend of a specific notification record. Platform permission. */
    public static final String NOTIFICATION_RESEND = "NOTIFICATION_RESEND";
}
