package com.sunday.service.content;

import com.sunday.common_lib.event.BookingCancelledEvent;
import com.sunday.common_lib.event.BookingConfirmedEvent;
import com.sunday.common_lib.event.PaymentFailedEvent;
import com.sunday.common_lib.event.PaymentRefundedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Builds email subject/HTML body per event type. Rendering only — EmailChannelProvider does the
 *  actual send. Kept as one class (not one-per-event-type) since every method is a short, independent
 *  subject/body pair and the shared date/money formatting helpers are genuinely shared. */
@Service
@RequiredArgsConstructor
public class EmailContentBuilder {

    private final TemplateEngine templateEngine;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    // ── Booking confirmed ───────────────────────────────────────────────────────

    public String subjectForBookingConfirmed(BookingConfirmedEvent event) {
        String depDate = event.getDepartureDateTime() != null ? event.getDepartureDateTime().format(DATE_FMT) : "";
        return String.format("Booking Confirmed | %s | %s→%s | %s",
                event.getBookingReference(), event.getDepartureAirportCode(), event.getArrivalAirportCode(), depDate);
    }

    public String bodyForBookingConfirmed(BookingConfirmedEvent event) {
        Context ctx = new Context(Locale.ENGLISH);
        ctx.setVariable("event", event);
        ctx.setVariable("passengerCount", event.getPassengers() != null ? event.getPassengers().size() : 1);

        ctx.setVariable("depDate", event.getDepartureDateTime() != null ? event.getDepartureDateTime().format(DATE_FMT) : "N/A");
        ctx.setVariable("depTime", event.getDepartureDateTime() != null ? event.getDepartureDateTime().format(TIME_FMT) : "N/A");
        ctx.setVariable("arrDate", event.getArrivalDateTime() != null ? event.getArrivalDateTime().format(DATE_FMT) : "N/A");
        ctx.setVariable("arrTime", event.getArrivalDateTime() != null ? event.getArrivalDateTime().format(TIME_FMT) : "N/A");
        ctx.setVariable("paidAt", event.getPaidAt() != null ? event.getPaidAt().format(DT_FMT) : "N/A");
        ctx.setVariable("bookingDate", event.getBookingDate() != null ? event.getBookingDate().format(DT_FMT) : "N/A");

        double base = orZero(event.getBaseFare());
        double taxes = orZero(event.getTaxesAndFees());
        double seats = orZero(event.getSeatFees());
        double ancillary = orZero(event.getAncillaryFees());
        double meals = orZero(event.getMealFees());
        double total = orZero(event.getTotalAmount());

        ctx.setVariable("baseFareTotal", fmt(base));
        ctx.setVariable("taxes", fmt(taxes));
        ctx.setVariable("seatFees", fmt(seats));
        ctx.setVariable("ancillaryFees", fmt(ancillary));
        ctx.setVariable("mealFees", fmt(meals));
        ctx.setVariable("totalAmount", fmt(total));

        ctx.setVariable("hasBaggage", event.getCheckinBaggagePieces() != null || event.getCabinBaggagePieces() != null);
        ctx.setVariable("checkinBaggage", baggageLabel(event.getCheckinBaggagePieces(), event.getCheckinBaggageWeightPerPiece()));
        ctx.setVariable("cabinBaggage", baggageLabel(event.getCabinBaggagePieces(), event.getCabinBaggageWeightPerPiece()));
        ctx.setVariable("cabinClassDisplay", cabinDisplayName(event.getCabinClass()));

        // Guaranteed non-null primitives, not the event's own nullable Boolean fields directly: SpringEL's
        // logical operators (!, and, or) throw on a null operand, and these ARE null whenever fare
        // enrichment fails upstream (BookingEventProducer.sendBookingConfirmed treats it as best-effort) —
        // a template crash here would be a real production incident, not a hypothetical.
        ctx.setVariable("freeDateChange", Boolean.TRUE.equals(event.getFreeDateChange()));
        ctx.setVariable("fullRefund", Boolean.TRUE.equals(event.getFullRefund()));
        ctx.setVariable("partialRefund", Boolean.TRUE.equals(event.getPartialRefund()));
        ctx.setVariable("priorityBoarding", Boolean.TRUE.equals(event.getPriorityBoarding()));
        ctx.setVariable("loungeAccess", Boolean.TRUE.equals(event.getLoungeAccess()));
        ctx.setVariable("complimentaryMeals", Boolean.TRUE.equals(event.getComplimentaryMeals()));

        return templateEngine.process("email/booking-confirmation", ctx);
    }

    // ── Booking cancelled ────────────────────────────────────────────────────────

    public String subjectForBookingCancelled(BookingCancelledEvent event) {
        return "Booking Cancelled | " + event.getBookingReference();
    }

    public String bodyForBookingCancelled(BookingCancelledEvent event) {
        Context ctx = new Context(Locale.ENGLISH);
        ctx.setVariable("bookingReference", event.getBookingReference());
        ctx.setVariable("reason", event.getReason());
        return templateEngine.process("email/booking-cancellation", ctx);
    }

    // ── Payment failed ───────────────────────────────────────────────────────────

    public String subjectForPaymentFailed(PaymentFailedEvent event) {
        return "Payment Failed | Booking #" + event.getBookingId();
    }

    public String bodyForPaymentFailed(PaymentFailedEvent event) {
        Context ctx = new Context(Locale.ENGLISH);
        ctx.setVariable("bookingId", event.getBookingId());
        ctx.setVariable("amount", event.getAmount() != null ? fmt(event.getAmount().doubleValue()) : "N/A");
        ctx.setVariable("currency", "NGN");
        ctx.setVariable("failureReason", event.getFailureReason());
        return templateEngine.process("email/payment-failed", ctx);
    }

    // ── Payment refunded ─────────────────────────────────────────────────────────

    public String subjectForPaymentRefunded(PaymentRefundedEvent event) {
        return "Refund Processed | Booking #" + event.getBookingId();
    }

    public String bodyForPaymentRefunded(PaymentRefundedEvent event) {
        Context ctx = new Context(Locale.ENGLISH);
        ctx.setVariable("bookingId", event.getBookingId());
        ctx.setVariable("amount", event.getAmount() != null ? fmt(event.getAmount().doubleValue()) : "N/A");
        ctx.setVariable("currency", "NGN");
        return templateEngine.process("email/payment-refunded", ctx);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static double orZero(Double v) {
        return v != null ? v : 0.0;
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }

    private static String baggageLabel(Integer pieces, Double weightPer) {
        if (pieces == null && weightPer == null) return "Not included";
        if (pieces != null && weightPer != null) return pieces + " × " + weightPer.intValue() + " kg";
        if (pieces != null) return pieces + " piece(s)";
        return weightPer.intValue() + " kg";
    }

    private static String cabinDisplayName(String cabinClass) {
        if (cabinClass == null) return "Economy";
        return switch (cabinClass) {
            case "ECONOMY" -> "Economy";
            case "PREMIUM_ECONOMY" -> "Premium Economy";
            case "BUSINESS" -> "Business";
            case "FIRST" -> "First Class";
            default -> cabinClass;
        };
    }
}
