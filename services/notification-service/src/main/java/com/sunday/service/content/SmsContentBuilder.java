package com.sunday.service.content;

import com.sunday.common_lib.event.BookingCancelledEvent;
import com.sunday.common_lib.event.BookingConfirmedEvent;
import com.sunday.common_lib.event.PaymentFailedEvent;
import com.sunday.common_lib.event.PaymentRefundedEvent;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Builds 160-char-friendly SMS bodies per event type. Plain formatted strings, not a templating
 *  engine — four short, independent messages don't justify a second Thymeleaf TEXT-mode resolver
 *  alongside email's HTML one. */
@Service
public class SmsContentBuilder {

    private static final DateTimeFormatter SMS_DT_FMT = DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale.ENGLISH);

    public String bodyForBookingConfirmed(BookingConfirmedEvent event) {
        int paxCount = event.getPassengers() != null ? event.getPassengers().size() : 1;
        String depStr = event.getDepartureDateTime() != null ? event.getDepartureDateTime().format(SMS_DT_FMT) : "N/A";
        String route = event.getDepartureAirportCode() + "→" + event.getArrivalAirportCode();
        String total = event.getTotalAmount() != null ? String.format("%.0f", event.getTotalAmount()) : "N/A";
        String currency = event.getCurrency() != null ? event.getCurrency() : "";

        return String.format(
                "ZoshAir: Booking CONFIRMED! Ref: %s | %s %s | %s | %d pax | %s %s paid. " +
                        "Check-in opens 48hrs before departure. Queries: support@zoshair.com",
                event.getBookingReference(), event.getFlightNumber() != null ? event.getFlightNumber() : "",
                route, depStr, paxCount, currency, total);
    }

    public String bodyForBookingCancelled(BookingCancelledEvent event) {
        return String.format("ZoshAir: Booking %s has been CANCELLED. Any held seats have been released. " +
                "Queries: support@zoshair.com", event.getBookingReference());
    }

    public String bodyForPaymentFailed(PaymentFailedEvent event) {
        return String.format("ZoshAir: Payment for booking #%s FAILED and the booking has been cancelled. " +
                "Please book again. Queries: support@zoshair.com", event.getBookingId());
    }

    public String bodyForPaymentRefunded(PaymentRefundedEvent event) {
        String amount = event.getAmount() != null ? event.getAmount().toPlainString() : "";
        String currency = event.getCurrency() != null ? event.getCurrency() : "";
        return String.format("ZoshAir: A refund of %s %s has been processed for booking #%s. It may take a few business " +
                "days to reflect in your account. Queries: support@zoshair.com", currency, amount, event.getBookingId());
    }
}
