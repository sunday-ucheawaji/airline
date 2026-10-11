package com.sunday.services.event.publisher;

import java.util.UUID;
import com.sunday.common_lib.dto.UserDTO;
import com.sunday.common_lib.event.BookingCancelledEvent;
import com.sunday.common_lib.event.BookingConfirmedEvent;
import com.sunday.common_lib.event.PassengerNotificationData;
import com.sunday.common_lib.payload.response.AirportResponse;
import com.sunday.common_lib.payload.response.BaggagePolicyResponse;
import com.sunday.common_lib.payload.response.CityResponse;
import com.sunday.common_lib.payload.response.FareResponse;
import com.sunday.common_lib.payload.response.FlightInstanceResponse;
import com.sunday.common_lib.event.PaymentCompletedEvent;
import com.sunday.services.model.Booking;
import com.sunday.services.model.Passenger;
import com.sunday.services.model.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendBookingConfirmed(Booking booking,
                                     PaymentCompletedEvent payment,
                                     FlightInstanceResponse flight,
                                     FareResponse fare,
                                     UserDTO user) {

        // ── Passengers ────────────────────────────────────────────────────────
        // Build a passengerId → ticketNumber lookup from the Ticket relation
        Map<UUID, String> ticketByPassenger = booking.getTickets().stream()
                .filter(t -> t.getPassenger() != null)
                .collect(Collectors.toMap(
                        t -> t.getPassenger().getId(),
                        Ticket::getTicketNumber,
                        (a, b) -> a   // keep first if duplicate
                ));

        List<PassengerNotificationData> passengers = booking.getPassengers().stream()
                .map(p -> PassengerNotificationData.builder()
                        .firstName(p.getFirstName())
                        .lastName(p.getLastName())
                        .ticketNumber(ticketByPassenger.getOrDefault(p.getId(), "N/A"))
                        .passportNumber(p.getPassportNumber())
                        .nationality(p.getNationality())
                        .gender(p.getGender() != null ? p.getGender().name() : "")
                        .adult(p.isAdult())
                        .frequentFlyerNumber(p.getFrequentFlyerNumber())
                        .requiresWheelchair(Boolean.TRUE.equals(p.getRequiresWheelchairAssistance()))
                        .dietaryPreferences(p.getDietaryPreferences())
                        .build())
                .collect(Collectors.toList());

        // ── Contact Info ──────────────────────────────────────────────────────
        String contactEmail = resolveContactEmail(booking);
        String contactPhone = resolveContactPhone(booking);

        // ── Flight Details ─────────────────────────────────────────────────────
        String flightNumber   = flight != null ? flight.getFlightNumber()  : "N/A";
        String airlineName    = flight != null ? flight.getAirlineName()   : "N/A";
        String airlineLogo    = flight != null ? flight.getAirlineLogo()   : null;
        String aircraftModel  = flight != null ? flight.getAircraftModal() : null;
        String terminal       = flight != null ? flight.getTerminal()      : null;
        String gate           = flight != null ? flight.getGate()          : null;
        String duration       = flight != null ? flight.getFormattedDuration() : null;
        LocalDateTime depTime = flight != null ? flight.getDepartureDateTime() : null;
        LocalDateTime arrTime = flight != null ? flight.getArrivalDateTime()   : null;

        AirportResponse dep    = flight != null ? flight.getDepartureAirport() : null;
        CityResponse    depCity = dep != null ? dep.getCity() : null;
        String depCode         = dep != null ? dep.getIataCode()        : "N/A";
        String depName         = dep != null ? dep.getName()            : "N/A";
        String depCityName     = depCity != null ? depCity.getName()    : "N/A";
        String depCountry      = depCity != null ? depCity.getCountryName() : "N/A";

        AirportResponse arr     = flight != null ? flight.getArrivalAirport() : null;
        CityResponse    arrCity = arr != null ? arr.getCity() : null;
        String arrCode          = arr != null ? arr.getIataCode()        : "N/A";
        String arrName          = arr != null ? arr.getName()            : "N/A";
        String arrCityName      = arrCity != null ? arrCity.getName()    : "N/A";
        String arrCountry       = arrCity != null ? arrCity.getCountryName() : "N/A";

        // ── Fare & Baggage ────────────────────────────────────────────────────
        // BookingConfirmedEvent is a display payload for notification-service's email/SMS templates, not part of
        // the settlement ledger, so it stays Double; the BigDecimal source values are converted here, once.
        String fareName         = fare != null ? fare.getName()                     : null;
        BigDecimal baseFare     = fare != null ? fare.getBaseFare()                 : null;
        BigDecimal taxes        = fare != null ? fare.getTaxesAndFees()             : null;
        BaggagePolicyResponse bag = fare != null ? fare.getBaggagePolicy()          : null;
        Integer ciPieces        = bag  != null ? bag.getCheckInBaggagePieces()     : null;
        Double  ciWeightPer     = bag  != null ? bag.getCheckInBaggageWeightPerPiece() : null;
        Integer cbPieces        = bag  != null ? bag.getCabinBaggagePieces()        : null;
        Double  cbWeightPer     = bag  != null ? bag.getCabinBaggageWeightPerPiece() : null;

        // ── Build & Publish ───────────────────────────────────────────────────
        BookingConfirmedEvent event = BookingConfirmedEvent.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .confirmedAt(LocalDateTime.now())
                .bookingDate(booking.getBookingDate())
                .cabinClass(booking.getCabinClass() != null ? booking.getCabinClass().name() : "ECONOMY")
                .tripType(booking.getTripType() != null ? booking.getTripType().name() : "ONE_WAY")
                .flexibleTicket(booking.isFlexibleTicket())
                // Contact
                .userId(booking.getUserId())
                .userName(user != null ? (user.getFirstName() + " " + user.getLastName()).trim() : "Valued Customer")
                .contactEmail(contactEmail)
                .contactPhone(contactPhone)
                // Passengers
                .passengers(passengers)
                // Flight
                .flightInstanceId(booking.getFlightInstanceId())
                .flightNumber(flightNumber)
                .airlineName(airlineName)
                .airlineLogo(airlineLogo)
                .aircraftModel(aircraftModel)
                .departureAirportCode(depCode)
                .departureAirportName(depName)
                .departureCity(depCityName)
                .departureCountry(depCountry)
                .departureTerminal(terminal)
                .departureGate(gate)
                .departureDateTime(depTime)
                .arrivalAirportCode(arrCode)
                .arrivalAirportName(arrName)
                .arrivalCity(arrCityName)
                .arrivalCountry(arrCountry)
                .arrivalDateTime(arrTime)
                .flightDuration(duration)
                // Payment
                .totalAmount(payment.getAmount() != null ? payment.getAmount().doubleValue() : null)
                .currency("INR")
                .transactionId(payment.getTransactionId())
                .providerPaymentId(payment.getProviderPaymentId())
                .paymentGateway("RAZORPAY")
                .paidAt(payment.getPaidAt())
                // Fare breakdown
                .fareName(fareName)
                .baseFare(baseFare != null ? baseFare.doubleValue() : null)
                .taxesAndFees(taxes != null ? taxes.doubleValue() : null)
                // Baggage
                .checkinBaggagePieces(ciPieces)
                .checkinBaggageWeightPerPiece(ciWeightPer)
                .cabinBaggagePieces(cbPieces)
                .cabinBaggageWeightPerPiece(cbWeightPer)
                // Policies
                .freeDateChange(fare != null ? fare.getFreeDateChange()       : null)
                .partialRefund(fare != null  ? fare.getPartialRefund()        : null)
                .fullRefund(fare != null     ? fare.getFullRefund()           : null)
                .priorityBoarding(fare != null ? fare.getPriorityBoarding()   : null)
                .loungeAccess(fare != null   ? fare.getLoungeAccess()         : null)
                .complimentaryMeals(fare != null ? fare.getComplimentaryMeals() : null)
                // Legacy — seat-service still reads this to mark seats BOOKED
                .seatInstanceIds(booking.getSeatInstanceIds())
                .build();

        kafkaTemplate.send("booking.confirmed", event);
        log.info("Published enriched BookingConfirmedEvent for booking={}", booking.getBookingReference());
    }

    /** A prompt-release optimization on top of seat-service's hold-expiry sweeper, not a substitute for it. */
    public void sendBookingCancelled(Booking booking) {
        sendBookingCancelled(booking.getId(), booking.getBookingReference(), booking.getSeatInstanceIds(),
                resolveContactEmail(booking), resolveContactPhone(booking), null);
    }

    public void sendBookingCancelled(Booking booking, String reason) {
        sendBookingCancelled(booking.getId(), booking.getBookingReference(), booking.getSeatInstanceIds(),
                resolveContactEmail(booking), resolveContactPhone(booking), reason);
    }

    /** Overload for compensation paths (e.g. BookingSagaRecoverySweeper) that only have the saga's own
     *  record of what was held, not a persisted Booking row — so no contact info is available either;
     *  notification-service treats a blank contact as "nothing to notify", not an error. */
    public void sendBookingCancelled(UUID bookingId, String bookingReference, List<UUID> seatInstanceIds) {
        sendBookingCancelled(bookingId, bookingReference, seatInstanceIds, null, null, null);
    }

    private void sendBookingCancelled(UUID bookingId, String bookingReference, List<UUID> seatInstanceIds,
                                       String contactEmail, String contactPhone, String reason) {
        if (seatInstanceIds == null || seatInstanceIds.isEmpty()) {
            return;
        }
        BookingCancelledEvent event = BookingCancelledEvent.builder()
                .bookingId(bookingId)
                .bookingReference(bookingReference)
                .seatInstanceIds(seatInstanceIds)
                .contactEmail(contactEmail)
                .contactPhone(contactPhone)
                .reason(reason)
                .build();
        kafkaTemplate.send("booking.cancelled", event);
        log.info("Published BookingCancelledEvent for booking={}", bookingReference);
    }

    /** Prefer booking.contactInfo; fall back to first passenger. Shared by sendBookingConfirmed/sendBookingCancelled. */
    private String resolveContactEmail(Booking booking) {
        String contactEmail = booking.getContactInfo() != null ? booking.getContactInfo().getEmail() : null;
        if (contactEmail == null || contactEmail.isBlank()) {
            contactEmail = booking.getPassengers().stream()
                    .map(Passenger::getEmail)
                    .filter(e -> e != null && !e.isBlank())
                    .findFirst().orElse(null);
        }
        return contactEmail;
    }

    private String resolveContactPhone(Booking booking) {
        String contactPhone = booking.getContactInfo() != null ? booking.getContactInfo().getPhone() : null;
        if (contactPhone == null || contactPhone.isBlank()) {
            contactPhone = booking.getPassengers().stream()
                    .map(Passenger::getPhone)
                    .filter(p -> p != null && !p.isBlank())
                    .findFirst().orElse(null);
        }
        return contactPhone;
    }
}
