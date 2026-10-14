package com.sunday.services.mapper;

import com.sunday.common_lib.payload.request.BookingRequest;
import com.sunday.common_lib.payload.response.AirlineResponse;
import com.sunday.common_lib.payload.response.AirportResponse;
import com.sunday.common_lib.payload.response.BookingResponse;
import com.sunday.common_lib.payload.response.FareResponse;
import com.sunday.common_lib.payload.response.FlightCabinAncillaryResponse;
import com.sunday.common_lib.payload.response.FlightInstanceResponse;
import com.sunday.common_lib.payload.response.FlightMealResponse;
import com.sunday.common_lib.payload.response.FlightResponse;
import com.sunday.common_lib.payload.response.PassengerResponse;
import com.sunday.common_lib.payload.response.PaymentDTO;
import com.sunday.common_lib.payload.response.SeatInstanceResponse;
import com.sunday.common_lib.payload.response.TicketResponse;
import com.sunday.services.model.Booking;
import com.sunday.services.model.Passenger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BookingMapper {

    public static Booking toEntity(BookingRequest request, UUID userId, Set<Passenger> passengers, String bookingReference) {
        return Booking.builder()
                .bookingReference(bookingReference)
                .userId(userId)
                .flightId(request.getFlightId())
                .flightInstanceId(request.getFlightInstanceId())
                .fareId(request.getFareId())
                .contactInfo(request.getContactInfo())
                .passengers(passengers)
                .bookingDate(LocalDateTime.now())
                .lastModified(LocalDateTime.now())
                .cabinClass(request.getCabinClass())
                .tripType(request.getTripType())
                .ancillaryIds(request.getAncillaryIds())
                .mealIds(request.getMealIds())
                .build();
    }

    public static BookingResponse toResponse(Booking booking,
                                             PaymentDTO payment,
                                             FareResponse fare,
                                             FlightResponse flight,
                                             FlightInstanceResponse flightInstance,
                                             List<FlightCabinAncillaryResponse> ancillaries,
                                             List<FlightMealResponse> meals,
                                             List<SeatInstanceResponse> seats) {
        List<PassengerResponse> passengers = booking.getPassengers().stream()
                .map(PassengerMapper::toResponse)
                .toList();
        List<TicketResponse> tickets = booking.getTickets().stream()
                .map(TicketMapper::toResponse)
                .toList();
        AirportResponse departure = flight != null ? flight.getDepartureAirport() : null;
        AirportResponse arrival = flight != null ? flight.getArrivalAirport() : null;
        AirlineResponse airline = flight != null ? flight.getAirline() : null;

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .userId(booking.getUserId())
                .flightId(booking.getFlightId())
                .flightNumber(flight != null ? flight.getFlightNumber() : null)
                .flightName(cityName(departure) != null && cityName(arrival) != null
                        ? cityName(departure) + " - " + cityName(arrival) : null)
                .departureTime(flightInstance != null ? flightInstance.getDepartureDateTime() : null)
                .arrivalTime(flightInstance != null ? flightInstance.getArrivalDateTime() : null)
                .flightDuration(flightInstance != null ? flightInstance.getFormattedDuration() : null)
                .departureAirport(departure != null ? departure.getName() : null)
                .arrivalAirport(arrival != null ? arrival.getName() : null)
                .status(booking.getStatus())
                .bookingDate(booking.getBookingDate())
                .lastModified(booking.getLastModified())
                .passengers(passengers)
                .tickets(tickets)
                .totalPassengers(passengers.size())
                .ancillaries(ancillaries)
                .meals(meals)
                .seatInstances(seats)
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .fareId(booking.getFareId())
                .fareName(fare != null ? fare.getName() : null)
                .fareBaseFare(booking.getFareBaseAmount())
                .fareTaxesAndFees(booking.getFareTaxAmount())
                .fareAirlineFees(booking.getFareFeeAmount())
                .totalAmount(booking.getTotalAmount())
                .refundAmount(booking.getRefundAmount())
                .fareChangeable(booking.getFareChangeable())
                .fareChangeFee(booking.getFareChangeFee())
                .fareChangeDeadlineHours(booking.getFareChangeDeadlineHours())
                .changeFeeAmount(booking.getChangeFeeAmount())
                .tripId(booking.getTripId())
                .exchangedFromBookingId(booking.getExchangedFromBookingId())
                .exchangedToBookingId(booking.getExchangedToBookingId())
                .currency(booking.getFareCurrency())
                .airlineName(airline != null ? airline.getName() : null)
                .airlineLogo(airline != null ? airline.getLogoUrl() : null)
                .contactInfo(booking.getContactInfo())
                .build();
    }

    private static String cityName(AirportResponse airport) {
        return airport != null && airport.getCity() != null ? airport.getCity().getName() : null;
    }
}
