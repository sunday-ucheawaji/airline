package com.sunday.services.service.impl;

import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.common_lib.event.PaymentCompletedEvent;
import com.sunday.common_lib.event.PaymentFailedEvent;
import com.sunday.common_lib.payload.request.SeatConfirmRequest;
import com.sunday.common_lib.payload.response.SeatConfirmResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.clients.FlightClient;
import com.sunday.services.clients.PaymentClient;
import com.sunday.services.clients.PricingClient;
import com.sunday.services.clients.SeatClient;
import com.sunday.services.clients.UserClient;
import com.sunday.services.event.publisher.BookingEventProducer;
import com.sunday.services.model.Booking;
import com.sunday.services.model.Passenger;
import com.sunday.services.repository.BookingRepository;
import com.sunday.services.service.BookingPaymentService;
import com.sunday.services.service.BookingSagaService;
import com.sunday.services.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingPaymentServiceImpl implements BookingPaymentService {

    private final BookingRepository bookingRepository;
    private final BookingEventProducer bookingEventProducer;
    private final BookingSagaService bookingSagaService;
    private final TicketService ticketService;
    private final FlightClient flightClient;
    private final PricingClient pricingClient;
    private final UserClient userClient;
    private final SeatClient seatClient;
    private final PaymentClient paymentClient;

    @Override
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        if (!bookingSagaService.claimPaymentEvent(event.getPaymentId(), "COMPLETED")) {
            return;
        }
        Booking booking = bookingRepository.findById(event.getBookingId()).orElse(null);
        if (booking == null) {
            log.error("Payment {} completed for unknown booking {}", event.getPaymentId(), event.getBookingId());
            return;
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED && event.getPaymentId().equals(booking.getPaymentId())) {
            log.info("Booking {} already confirmed by payment {}", booking.getBookingReference(), event.getPaymentId());
            return;
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            paymentClient.refundPaymentAsSystem(event.getPaymentId(), null);
            log.warn("Payment {} arrived for booking {} in status {} and was refunded",
                    event.getPaymentId(), booking.getBookingReference(), booking.getStatus());
            return;
        }
        if (!amountMatches(booking.getTotalAmount(), event.getAmount())
                || (event.getCurrency() != null && !event.getCurrency().equals(booking.getFareCurrency()))) {
            log.error("Payment {} of {} {} does not match booking {} total {} {}",
                    event.getPaymentId(), event.getAmount(), event.getCurrency(),
                    booking.getBookingReference(), booking.getTotalAmount(), booking.getFareCurrency());
            cancelAndRefund(booking, event.getPaymentId(), ErrorMessageUtil.BOOKING_CANCELLED_AMOUNT_MISMATCH);
            return;
        }

        SeatConfirmResponse seats = confirmSeats(booking);
        if (seats != null && !seats.isAllocated()) {
            cancelAndRefund(booking, event.getPaymentId(), ErrorMessageUtil.BOOKING_CANCELLED_NO_SEATS);
            return;
        }
        boolean seatsReassigned = seats != null && !seats.getReassigned().isEmpty();
        if (seatsReassigned) {
            applyReassignedSeats(booking, seats);
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentId(event.getPaymentId());
        Booking confirmed = bookingRepository.save(booking);
        ticketService.generateTicketsForBooking(confirmed);
        bookingSagaService.complete(confirmed.getId());
        log.info("Booking {} confirmed after payment {}", confirmed.getBookingReference(), event.getPaymentId());

        bookingEventProducer.sendBookingConfirmed(confirmed, event,
                fetchQuietly("flight instance", () -> flightClient.getFlightInstanceResponse(confirmed.getFlightInstanceId())),
                fetchQuietly("fare", () -> pricingClient.getFareById(confirmed.getFareId())),
                fetchQuietly("user", () -> userClient.getUserById(confirmed.getUserId())),
                seatsReassigned);
    }

    @Override
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        if (!bookingSagaService.claimPaymentEvent(event.getPaymentId(), "FAILED")) {
            return;
        }
        Booking booking = bookingRepository.findById(event.getBookingId()).orElse(null);
        if (booking == null || booking.getStatus() != BookingStatus.PENDING) {
            log.info("Ignoring failed payment {} for booking {} that is not pending", event.getPaymentId(), event.getBookingId());
            return;
        }
        booking.setStatus(BookingStatus.CANCELLED);
        Booking cancelled = bookingRepository.save(booking);
        bookingEventProducer.sendBookingCancelled(cancelled,
                String.format(ErrorMessageUtil.BOOKING_CANCELLED_PAYMENT_FAILED, event.getFailureReason()));
        bookingSagaService.cancel(cancelled.getId());
        log.warn("Booking {} cancelled after payment failure: {}", cancelled.getBookingReference(), event.getFailureReason());
    }

    private static boolean amountMatches(BigDecimal expected, BigDecimal paid) {
        return expected != null && paid != null
                && paid.setScale(2, RoundingMode.HALF_EVEN).compareTo(expected.setScale(2, RoundingMode.HALF_EVEN)) == 0;
    }

    private SeatConfirmResponse confirmSeats(Booking booking) {
        if (booking.getSeatInstanceIds() == null || booking.getSeatInstanceIds().isEmpty()) {
            return null;
        }
        return seatClient.confirmSeats(SeatConfirmRequest.builder()
                .flightInstanceId(booking.getFlightInstanceId())
                .cabinClass(booking.getCabinClass())
                .bookingReference(booking.getBookingReference())
                .seatInstanceIds(booking.getSeatInstanceIds())
                .build());
    }

    private static void applyReassignedSeats(Booking booking, SeatConfirmResponse seats) {
        Map<UUID, UUID> reassigned = seats.getReassigned();
        booking.setSeatInstanceIds(new ArrayList<>(seats.getSeatInstanceIds()));
        for (Passenger passenger : booking.getPassengers()) {
            UUID replacement = reassigned.get(passenger.getSeatInstanceId());
            if (replacement != null) {
                passenger.setSeatInstanceId(replacement);
            }
        }
        log.warn("Booking {} had seats reassigned at confirmation: {}", booking.getBookingReference(), reassigned);
    }

    private void cancelAndRefund(Booking booking, UUID paymentId, String reason) {
        paymentClient.refundPaymentAsSystem(paymentId, null);
        booking.setPaymentId(paymentId);
        booking.setStatus(BookingStatus.CANCELLED);
        Booking cancelled = bookingRepository.save(booking);
        bookingEventProducer.sendBookingCancelled(cancelled, reason);
        bookingSagaService.cancel(cancelled.getId());
        log.warn("Booking {} cancelled and payment {} refunded: {}", cancelled.getBookingReference(), paymentId, reason);
    }

    private static <T> T fetchQuietly(String what, Supplier<T> lookup) {
        try {
            return lookup.get();
        } catch (RuntimeException e) {
            log.warn("Could not fetch {} for the confirmation notice: {}", what, e.getMessage());
            return null;
        }
    }
}
