package com.sunday.services.service.impl;

import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.common_lib.enums.TicketStatus;
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
import com.sunday.services.model.ExtrasOrder;
import com.sunday.services.model.Passenger;
import com.sunday.services.model.Trip;
import com.sunday.services.repository.BookingRepository;
import com.sunday.services.repository.ExtrasOrderRepository;
import com.sunday.services.repository.TripRepository;
import com.sunday.services.service.BookingPaymentService;
import com.sunday.services.service.BookingSagaService;
import com.sunday.services.service.ExtrasOrderService;
import com.sunday.services.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final TripRepository tripRepository;
    private final ExtrasOrderRepository extrasOrderRepository;
    private final ExtrasOrderService extrasOrderService;

    @Override
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        if (!bookingSagaService.claimPaymentEvent(event.getPaymentId(), "COMPLETED")) {
            return;
        }
        Booking booking = bookingRepository.findById(event.getBookingId()).orElse(null);
        if (booking == null) {
            Trip trip = tripRepository.findById(event.getBookingId()).orElse(null);
            if (trip != null) {
                completeTrip(trip, event);
                return;
            }
            ExtrasOrder order = extrasOrderRepository.findById(event.getBookingId()).orElse(null);
            if (order != null) {
                extrasOrderService.handlePaid(order, event);
                return;
            }
            log.error("Payment {} completed for unknown booking {}", event.getPaymentId(), event.getBookingId());
            return;
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED && event.getPaymentId().equals(booking.getPaymentId())) {
            log.info("Booking {} already confirmed by payment {}", booking.getBookingReference(), event.getPaymentId());
            return;
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            paymentClient.refundPaymentAsSystem(event.getPaymentId(), null, "reject:" + event.getPaymentId());
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
        confirm(booking, event, seats);
    }

    /**
     * A trip is confirmed as a whole: every flight's seats are booked first, and if any flight's cabin is full the
     * whole payment is refunded and every flight cancelled (seats already booked on earlier flights are released).
     */
    private void completeTrip(Trip trip, PaymentCompletedEvent event) {
        if (trip.getStatus() == BookingStatus.CONFIRMED && event.getPaymentId().equals(trip.getPaymentId())) {
            return;
        }
        List<Booking> legs = bookingRepository.findByTripId(trip.getId()).stream()
                .sorted(Comparator.comparing(Booking::getFareTravelDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (trip.getStatus() != BookingStatus.PENDING || legs.isEmpty()
                || legs.stream().anyMatch(leg -> leg.getStatus() != BookingStatus.PENDING)) {
            rejectTrip(trip, legs, event.getPaymentId(), ErrorMessageUtil.BOOKING_CANCELLED_PAYMENT_TIMEOUT);
            return;
        }
        if (!amountMatches(trip.getTotalAmount(), event.getAmount())
                || (event.getCurrency() != null && !event.getCurrency().equals(trip.getCurrency()))) {
            rejectTrip(trip, legs, event.getPaymentId(), ErrorMessageUtil.BOOKING_CANCELLED_AMOUNT_MISMATCH);
            return;
        }
        Map<Booking, SeatConfirmResponse> seats = new LinkedHashMap<>();
        for (Booking leg : legs) {
            SeatConfirmResponse legSeats = confirmSeats(leg);
            if (legSeats != null && !legSeats.isAllocated()) {
                rejectTrip(trip, legs, event.getPaymentId(), ErrorMessageUtil.BOOKING_CANCELLED_NO_SEATS);
                return;
            }
            seats.put(leg, legSeats);
        }
        seats.forEach((leg, legSeats) -> confirm(leg, event, legSeats));
        trip.setStatus(BookingStatus.CONFIRMED);
        trip.setPaymentId(event.getPaymentId());
        tripRepository.save(trip);
        log.info("Trip {} confirmed after payment {}", trip.getTripReference(), event.getPaymentId());
    }

    private void rejectTrip(Trip trip, List<Booking> legs, UUID paymentId, String reason) {
        paymentClient.refundPaymentAsSystem(paymentId, null, "reject:" + paymentId);
        for (Booking leg : legs) {
            if (leg.getStatus() == BookingStatus.PENDING) {
                leg.setPaymentId(paymentId);
                leg.setStatus(BookingStatus.CANCELLED);
                Booking cancelled = bookingRepository.save(leg);
                bookingEventProducer.sendBookingCancelled(cancelled, reason);
                bookingSagaService.cancel(cancelled.getId());
            }
        }
        if (trip.getStatus() == BookingStatus.PENDING) {
            trip.setStatus(BookingStatus.CANCELLED);
        }
        tripRepository.save(trip);
        log.warn("Payment {} for trip {} was refunded: {}", paymentId, trip.getTripReference(), reason);
    }

    private void confirm(Booking booking, PaymentCompletedEvent event, SeatConfirmResponse seats) {
        boolean seatsReassigned = seats != null && !seats.getReassigned().isEmpty();
        if (seatsReassigned) {
            applyReassignedSeats(booking, seats);
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentId(event.getPaymentId());
        Booking confirmed = bookingRepository.save(booking);
        ticketService.generateTicketsForBooking(confirmed);
        completeExchange(confirmed);
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
        if (booking == null) {
            Trip trip = tripRepository.findById(event.getBookingId()).orElse(null);
            if (trip != null && trip.getStatus() == BookingStatus.PENDING) {
                for (Booking leg : bookingRepository.findByTripId(trip.getId())) {
                    if (leg.getStatus() == BookingStatus.PENDING) {
                        leg.setStatus(BookingStatus.CANCELLED);
                        Booking cancelled = bookingRepository.save(leg);
                        bookingEventProducer.sendBookingCancelled(cancelled,
                                String.format(ErrorMessageUtil.BOOKING_CANCELLED_PAYMENT_FAILED, event.getFailureReason()));
                        bookingSagaService.cancel(cancelled.getId());
                    }
                }
                trip.setStatus(BookingStatus.CANCELLED);
                tripRepository.save(trip);
                return;
            }
            extrasOrderRepository.findById(event.getBookingId()).ifPresent(extrasOrderService::handleFailed);
            return;
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
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

    /**
     * The replacement of a voluntary change is paid (its price includes the change fees): the original is cancelled
     * and refunded its value, all of it for a refundable fare, otherwise only what the replacement uses. The refund
     * goes first, so a failure rolls the confirmation back for a redelivery.
     */
    private void completeExchange(Booking replacement) {
        if (replacement.getExchangedFromBookingId() == null) {
            return;
        }
        Booking original = bookingRepository.findById(replacement.getExchangedFromBookingId()).orElse(null);
        if (original == null || original.getStatus() != BookingStatus.CONFIRMED) {
            log.warn("Booking {} replaces booking {}, which is no longer confirmed; nothing to exchange",
                    replacement.getBookingReference(), replacement.getExchangedFromBookingId());
            return;
        }
        BigDecimal refund = original.exchangeRefund(replacement.refundableValue());
        ticketService.closeTicketsForBooking(original, refund.signum() > 0 ? TicketStatus.REFUNDED : TicketStatus.CANCELLED);
        if (refund.signum() > 0) {
            paymentClient.refundPaymentAsSystem(original.getPaymentId(), refund, "exchange:" + original.getId());
            original.setRefundAmount(refund);
        }
        extrasOrderService.closeOrders(original, Boolean.TRUE.equals(original.getFareRefundable()));
        original.setStatus(BookingStatus.CANCELLED);
        original.setExchangedToBookingId(replacement.getId());
        Booking exchanged = bookingRepository.save(original);
        bookingEventProducer.sendBookingCancelled(exchanged,
                String.format(ErrorMessageUtil.BOOKING_EXCHANGED, replacement.getBookingReference()));
        bookingSagaService.cancel(exchanged.getId());
        log.info("Booking {} exchanged for {}, refunded {}", exchanged.getBookingReference(), replacement.getBookingReference(), refund);
    }

    private void cancelAndRefund(Booking booking, UUID paymentId, String reason) {
        paymentClient.refundPaymentAsSystem(paymentId, null, "reject:" + paymentId);
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
