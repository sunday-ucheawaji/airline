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
import com.sunday.services.config.BookingPaymentProperties;
import com.sunday.services.model.Booking;
import com.sunday.services.model.Passenger;
import com.sunday.services.model.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingEventProducer {

    private static final String BOOKING_CONFIRMED = "booking.confirmed";
    private static final String BOOKING_CANCELLED = "booking.cancelled";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final BookingPaymentProperties paymentProperties;

    public void sendBookingConfirmed(Booking booking,
                                     PaymentCompletedEvent payment,
                                     FlightInstanceResponse flight,
                                     FareResponse fare,
                                     UserDTO user,
                                     boolean seatsReassigned) {

        Map<UUID, String> ticketByPassenger = booking.getTickets().stream()
                .filter(t -> t.getPassenger() != null)
                .collect(Collectors.toMap(
                        t -> t.getPassenger().getId(),
                        Ticket::getTicketNumber,
                        (a, b) -> a
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

        String contactEmail = resolveContactEmail(booking);
        String contactPhone = resolveContactPhone(booking);

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

        String fareName         = fare != null ? fare.getName()                     : null;
        BigDecimal baseFare     = fare != null ? fare.getBaseFare()                 : null;
        BigDecimal taxes        = fare != null ? fare.getTaxesAndFees()             : null;
        BaggagePolicyResponse bag = fare != null ? fare.getBaggagePolicy()          : null;
        Integer ciPieces        = bag  != null ? bag.getCheckInBaggagePieces()     : null;
        Double  ciWeightPer     = bag  != null ? bag.getCheckInBaggageWeightPerPiece() : null;
        Integer cbPieces        = bag  != null ? bag.getCabinBaggagePieces()        : null;
        Double  cbWeightPer     = bag  != null ? bag.getCabinBaggageWeightPerPiece() : null;

        BookingConfirmedEvent event = BookingConfirmedEvent.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .confirmedAt(LocalDateTime.now())
                .bookingDate(booking.getBookingDate())
                .cabinClass(booking.getCabinClass() != null ? booking.getCabinClass().name() : "ECONOMY")
                .tripType(booking.getTripType() != null ? booking.getTripType().name() : "ONE_WAY")
                .flexibleTicket(booking.isFlexibleTicket())
                .userId(booking.getUserId())
                .userName(user != null ? (user.getFirstName() + " " + user.getLastName()).trim() : "Valued Customer")
                .contactEmail(contactEmail)
                .contactPhone(contactPhone)
                .passengers(passengers)
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
                .totalAmount(payment.getAmount() != null ? payment.getAmount().doubleValue() : null)
                .currency(booking.getFareCurrency())
                .transactionId(payment.getTransactionId())
                .providerPaymentId(payment.getProviderPaymentId())
                .paymentGateway(paymentProperties.gatewayFor(booking.getFareCurrency()).name())
                .paidAt(payment.getPaidAt())
                .fareName(fareName)
                .baseFare(baseFare != null ? baseFare.doubleValue() : null)
                .taxesAndFees(taxes != null ? taxes.doubleValue() : null)
                .checkinBaggagePieces(ciPieces)
                .checkinBaggageWeightPerPiece(ciWeightPer)
                .cabinBaggagePieces(cbPieces)
                .cabinBaggageWeightPerPiece(cbWeightPer)
                .freeDateChange(fare != null ? fare.getFreeDateChange()       : null)
                .partialRefund(fare != null  ? fare.getPartialRefund()        : null)
                .fullRefund(fare != null     ? fare.getFullRefund()           : null)
                .priorityBoarding(fare != null ? fare.getPriorityBoarding()   : null)
                .loungeAccess(fare != null   ? fare.getLoungeAccess()         : null)
                .complimentaryMeals(fare != null ? fare.getComplimentaryMeals() : null)
                .seatInstanceIds(booking.getSeatInstanceIds())
                .seatsReassigned(seatsReassigned)
                .build();

        publishAfterCommit(BOOKING_CONFIRMED, booking.getBookingReference(), event);
    }

    public void sendBookingCancelled(Booking booking, String reason) {
        sendBookingCancelled(booking.getId(), booking.getBookingReference(), booking.getSeatInstanceIds(),
                resolveContactEmail(booking), resolveContactPhone(booking), reason);
    }

    public void sendSeatsReleased(UUID bookingId, String bookingReference, List<UUID> seatInstanceIds) {
        UUID eventBookingId = bookingId != null
                ? bookingId : UUID.nameUUIDFromBytes(bookingReference.getBytes(StandardCharsets.UTF_8));
        sendBookingCancelled(eventBookingId, bookingReference, seatInstanceIds, null, null, null);
    }

    private void sendBookingCancelled(UUID bookingId, String bookingReference, List<UUID> seatInstanceIds,
                                      String contactEmail, String contactPhone, String reason) {
        if (bookingReference == null || seatInstanceIds == null || seatInstanceIds.isEmpty()) {
            return;
        }
        BookingCancelledEvent event = BookingCancelledEvent.builder()
                .bookingId(bookingId)
                .bookingReference(bookingReference)
                .seatInstanceIds(List.copyOf(seatInstanceIds))
                .contactEmail(contactEmail)
                .contactPhone(contactPhone)
                .reason(reason)
                .build();
        publishAfterCommit(BOOKING_CANCELLED, bookingReference, event);
    }

    private void publishAfterCommit(String topic, String key, Object event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish(topic, key, event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish(topic, key, event);
            }
        });
    }

    private void publish(String topic, String key, Object event) {
        kafkaTemplate.send(topic, key, event);
        log.info("Published {} for booking {}", topic, key);
    }

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
