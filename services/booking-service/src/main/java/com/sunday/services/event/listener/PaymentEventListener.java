package com.sunday.services.event.listener;

import java.util.ArrayList;
import com.sunday.services.clients.PaymentClient;
import com.sunday.services.clients.SeatClient;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.common_lib.payload.response.SeatConfirmResponse;
import com.sunday.common_lib.payload.request.SeatConfirmRequest;
import java.util.UUID;
import com.sunday.common_lib.dto.UserDTO;
import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.common_lib.event.PaymentCompletedEvent;
import com.sunday.common_lib.event.PaymentFailedEvent;
import com.sunday.common_lib.payload.response.FareResponse;
import com.sunday.common_lib.payload.response.FlightInstanceResponse;
import com.sunday.services.clients.FlightClient;
import com.sunday.services.clients.PricingClient;
import com.sunday.services.clients.UserClient;
import com.sunday.services.event.publisher.BookingEventProducer;
import com.sunday.services.model.Booking;
import com.sunday.services.repository.BookingRepository;
import com.sunday.services.service.BookingSagaService;
import com.sunday.services.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final BookingRepository bookingRepository;
    private final BookingEventProducer bookingEventProducer;
    private final BookingSagaService bookingSagaService;
    private final TicketService ticketService;
    private final FlightClient flightClient;
    private final PricingClient pricingClient;
    private final UserClient userClient;
    private final SeatClient seatClient;
    private final PaymentClient paymentClient;

    @KafkaListener(topics = "payment.completed", groupId = "booking-service-group")
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent for bookingId={}", event.getBookingId());

        if (!bookingSagaService.claimPaymentEvent(event.getPaymentId(), "COMPLETED")) {
            return;
        }

        Booking booking = bookingRepository.findById(event.getBookingId()).orElse(null);
        if (booking == null) {
            log.error("Booking not found for id={}", event.getBookingId());
            return;
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            log.info("Booking {} already confirmed, ignoring redelivered payment {}", booking.getBookingReference(), event.getPaymentId());
            return;
        }
        // Defence in depth: payment-service already checks the amount. A mismatch is never confirmed.
        if (booking.getTotalAmount() == null || event.getAmount() == null
                || event.getAmount().setScale(2, RoundingMode.HALF_EVEN)
                        .compareTo(booking.getTotalAmount().setScale(2, RoundingMode.HALF_EVEN)) != 0) {
            log.error("Payment {} amount {} does not match booking {} total {}; booking not confirmed",
                    event.getPaymentId(), event.getAmount(), booking.getBookingReference(), booking.getTotalAmount());
            return;
        }

        SeatConfirmResponse seats = confirmSeats(booking);
        if (seats != null && !seats.isAllocated()) {
            cancelAndRefund(booking, event);
            return;
        }
        boolean seatsReassigned = seats != null && !seats.getReassigned().isEmpty();
        if (seatsReassigned) {
            booking.setSeatInstanceIds(new ArrayList<>(seats.getSeatInstanceIds()));
            log.warn("Booking {} had seats reassigned at confirmation: {}", booking.getBookingReference(), seats.getReassigned());
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentId(event.getPaymentId());
        booking = bookingRepository.save(booking);
        ticketService.generateTicketsForBooking(booking);
        bookingSagaService.complete(booking.getId());
        log.info("Booking {} confirmed after payment {}", booking.getBookingReference(), event.getPaymentId());

        // Fetch enrichment data for notification — failures are non-fatal
        FlightInstanceResponse flightInstance = fetchFlightInstance(booking.getFlightInstanceId());
        FareResponse fareResponse = fetchFare(booking.getFareId());
        UserDTO userDTO = fetchUser(booking.getUserId());

        // Publish enriched event (seat-service + notification-service both consume it)
        bookingEventProducer.sendBookingConfirmed(booking, event, flightInstance, fareResponse, userDTO, seatsReassigned);
    }

    @KafkaListener(topics = "payment.failed", groupId = "booking-service-group")
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent for bookingId={}", event.getBookingId());

        if (!bookingSagaService.claimPaymentEvent(event.getPaymentId(), "FAILED")) {
            return;
        }

        Booking booking = bookingRepository.findById(event.getBookingId()).orElse(null);
        if (booking == null) {
            log.error("Booking not found for id={}", event.getBookingId());
            return;
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.info("Booking {} already cancelled, ignoring redelivered payment failure {}",
                    booking.getBookingReference(), event.getPaymentId());
            return;
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);
        bookingEventProducer.sendBookingCancelled(booking, "Payment failed: " + event.getFailureReason());
        bookingSagaService.cancel(booking.getId());
        log.warn("Booking {} cancelled due to payment failure: {}",
                booking.getBookingReference(), event.getFailureReason());
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Books the held seats in seat-service, which replaces any seat lost to another booking since the hold expired.
     * An unreachable seat-service throws, so the whole payment event is retried (the dedup claim rolls back with it).
     */
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

    /** The cabin was full: the customer paid for seats that no longer exist, so the money goes back first. */
    private void cancelAndRefund(Booking booking, PaymentCompletedEvent event) {
        paymentClient.refundPaymentAsSystem(event.getPaymentId());
        booking.setPaymentId(event.getPaymentId());
        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);
        bookingEventProducer.sendBookingCancelled(booking, ErrorMessageUtil.BOOKING_CANCELLED_NO_SEATS);
        bookingSagaService.cancel(booking.getId());
        log.warn("Booking {} cancelled and payment {} refunded: no seat left in the {} cabin",
                booking.getBookingReference(), event.getPaymentId(), booking.getCabinClass());
    }

    private FlightInstanceResponse fetchFlightInstance(UUID flightInstanceId) {
        if (flightInstanceId == null) return null;
        try {
            return flightClient.getFlightInstanceResponse(flightInstanceId);
        } catch (Exception e) {
            log.warn("Could not fetch FlightInstance id={} for notification enrichment: {}",
                    flightInstanceId, e.getMessage());
            return null;
        }
    }

    private FareResponse fetchFare(UUID fareId) {
        if (fareId == null) return null;
        try {
            return pricingClient.getFareById(fareId);
        } catch (Exception e) {
            log.warn("Could not fetch Fare id={} for notification enrichment: {}",
                    fareId, e.getMessage());
            return null;
        }
    }

    private UserDTO fetchUser(UUID userId) {
        if (userId == null) return null;
        try {
            return userClient.getUserById(userId);
        } catch (Exception e) {
            log.warn("Could not fetch User id={} for notification enrichment: {}",
                    userId, e.getMessage());
            return null;
        }
    }
}
