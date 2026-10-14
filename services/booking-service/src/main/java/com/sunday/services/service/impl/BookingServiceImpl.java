package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.BookingPermissions;
import com.sunday.common_lib.enums.AirlineStatus;
import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.common_lib.enums.PassengerType;
import com.sunday.common_lib.enums.TicketStatus;
import com.sunday.common_lib.enums.TripType;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.payload.request.BookingChangeRequest;
import com.sunday.common_lib.payload.request.BookingRequest;
import com.sunday.common_lib.payload.request.TripRequest;
import com.sunday.common_lib.payload.request.TripSegmentRequest;
import com.sunday.common_lib.payload.request.PassengerRequest;
import com.sunday.common_lib.payload.request.PaymentInitiateRequest;
import com.sunday.common_lib.payload.request.SeatHoldRequest;
import com.sunday.common_lib.payload.response.*;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.clients.*;
import com.sunday.services.config.BookingPaymentProperties;
import com.sunday.services.event.publisher.BookingEventProducer;
import com.sunday.services.integration.PricingIntegrationService;
import com.sunday.services.mapper.BookingMapper;
import com.sunday.services.mapper.ExtrasOrderMapper;
import com.sunday.services.mapper.PassengerMapper;
import com.sunday.services.model.Booking;
import com.sunday.services.model.ExtrasOrder;
import com.sunday.services.model.BookingSaga;
import com.sunday.services.model.Passenger;
import com.sunday.services.model.SagaStatus;
import com.sunday.services.model.Trip;
import com.sunday.services.repository.BookingRepository;
import com.sunday.services.repository.CurrencyTotal;
import com.sunday.services.repository.ExtrasOrderRepository;
import com.sunday.services.repository.TripRepository;
import com.sunday.services.repository.DailyTotal;
import com.sunday.services.service.BookingAccessService;
import com.sunday.services.service.BookingSagaService;
import com.sunday.services.service.BookingService;
import com.sunday.services.service.ExtrasOrderService;
import com.sunday.services.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.hibernate.Hibernate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int TREND_DAYS = 30;
    private static final int MAX_TRIP_FLIGHTS = 6;
    private static final int TREND_MONTHS = 12;

    private final BookingRepository bookingRepository;
    private final BookingSagaService bookingSagaService;
    private final TicketService ticketService;
    private final PricingIntegrationService pricingIntegrationService;
    private final PricingClient pricingClient;
    private final AncillaryClient ancillaryClient;
    private final PaymentClient paymentClient;
    private final SeatClient seatClient;
    private final FlightClient flightClient;
    private final BookingAccessService bookingAccess;
    private final BookingEventProducer bookingEventProducer;
    private final BookingPaymentProperties paymentProperties;
    private final Clock clock;
    private final PlatformTransactionManager transactionManager;
    private final TripRepository tripRepository;
    private final ExtrasOrderRepository extrasOrderRepository;
    private final ExtrasOrderService extrasOrderService;

    @Override
    public PaymentInitiateResponse createBooking(BookingRequest request, UUID userId, String idempotencyKey) {
        return startBooking(request, userId, idempotencyKey, null);
    }

    @Override
    public PaymentInitiateResponse changeBooking(UUID id, BookingChangeRequest request, UUID userId, String idempotencyKey) {
        Booking original = changeableBooking(id, userId);
        return startBooking(rebookingRequest(original, request), original.getUserId(), idempotencyKey, original);
    }

    @Override
    public BookingChangeQuoteResponse quoteChange(UUID id, BookingChangeRequest request, UUID userId) {
        Booking original = changeableBooking(id, userId);
        BookingRequest rebooking = rebookingRequest(original, request);
        List<UUID> seatInstanceIds = requireDistinctSeats(rebooking.getPassengers());
        Pricing pricing = price(rebooking, original);

        BigDecimal fares = rebooking.getPassengers().stream().map(pricing::fareFor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal seatPremiums = Objects.requireNonNullElse(seatClient.calculateSeatPrice(seatInstanceIds), BigDecimal.ZERO);
        BigDecimal changeFees = original.changeFees();
        BigDecimal newTotal = fares.add(seatPremiums).add(pricing.ancillaryPrice()).add(pricing.mealPrice()).add(changeFees)
                .setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal originalRefund = original.exchangeRefund(newTotal.subtract(changeFees));
        return BookingChangeQuoteResponse.builder()
                .currency(pricing.adultQuote().getCurrency())
                .fares(fares)
                .seatPremiums(seatPremiums)
                .ancillaries(pricing.ancillaryPrice())
                .meals(pricing.mealPrice())
                .changeFees(changeFees)
                .newTotal(newTotal)
                .originalRefund(originalRefund)
                .netCost(newTotal.subtract(originalRefund))
                .build();
    }

    private Booking changeableBooking(UUID id, UUID userId) {
        Booking original = bookingRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.BOOKING_NOT_FOUND_BY_ID, id)));
        bookingAccess.requireOwnerOrPermission(original, userId, BookingPermissions.BOOKING_MANAGE);
        requireChangeable(original);
        return original;
    }

    /** Everything a booking costs before its seats are held: the departure, a quote per fare type, and the extras. */
    private record Pricing(UUID flightId, LocalDate departureDate, FlightResponse flight,
                           Map<PassengerType, FareQuoteResponse> quotes, BigDecimal ancillaryPrice, BigDecimal mealPrice) {

        FareQuoteResponse adultQuote() {
            return quotes.get(PassengerType.ADT);
        }

        BigDecimal fareFor(PassengerRequest passenger) {
            PassengerType type = Passenger.typeOn(passenger.getDateOfBirth(), departureDate);
            return quotes.get(Passenger.pricingType(type, passenger.getSeatInstanceId())).getTotalPrice();
        }
    }

    private Pricing price(BookingRequest request, Booking exchangedFrom) {
        FlightInstanceResponse flightInstanceResponse = flightClient.getFlightInstanceResponse(request.getFlightInstanceId());
        UUID flightId = flightInstanceResponse.getFlightId();
        if (request.getFlightId() != null && !request.getFlightId().equals(flightId)) {
            throw new BadRequestException(String.format(
                    ErrorMessageUtil.FLIGHT_ID_DOES_NOT_MATCH_INSTANCE, request.getFlightId(), request.getFlightInstanceId()));
        }
        LocalDate departureDate = flightInstanceResponse.getDepartureDateTime().toLocalDate();
        FlightResponse flightResponse = flightClient.getFlightById(flightId);
        requireAirlineAcceptingBookings(flightResponse);
        if (exchangedFrom != null) {
            requireSameAirline(exchangedFrom, flightResponse);
        }

        Map<PassengerType, FareQuoteResponse> quotes = quoteByPassengerType(request, flightId, departureDate);
        String currency = quotes.get(PassengerType.ADT).getCurrency();
        if (paymentProperties.gatewayFor(currency) == null) {
            throw new ConflictException(String.format(ErrorMessageUtil.BOOKING_CURRENCY_NOT_ACCEPTED, currency));
        }

        List<UUID> ancillaryIds = request.getAncillaryIds();
        BigDecimal ancillaryPrice = ancillaryIds == null || ancillaryIds.isEmpty()
                ? BigDecimal.ZERO : ancillaryClient.calculateAncillariesPrice(
                        flightId, request.getCabinClass(), request.getPassengers().size(), ancillaryIds);
        List<UUID> mealIds = request.getMealIds();
        BigDecimal mealPrice = mealIds == null || mealIds.isEmpty()
                ? BigDecimal.ZERO : ancillaryClient.calculateMealPrice(flightId, request.getPassengers().size(), mealIds);
        return new Pricing(flightId, departureDate, flightResponse, quotes, ancillaryPrice, mealPrice);
    }

    /** Creates a booking; {@code exchangedFrom} is the confirmed booking it replaces, or null for a new sale. */
    private PaymentInitiateResponse startBooking(BookingRequest request, UUID userId, String idempotencyKey, Booking exchangedFrom) {
        String key = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();
        BookingSaga saga = beginSaga(request, userId, key);
        if (paymentInitiated(saga)) {
            log.info("Idempotency-Key {} already initiated payment, returning the original response", key);
            return bookingSagaService.readResponse(saga);
        }
        requireStarted(saga, key);

        Booking saved = reserve(request, userId, saga, exchangedFrom, exchangedFrom != null ? exchangedFrom.getTripId() : null);
        try {
            PaymentInitiateResponse paymentInit = initiatePayment(saved.getId(), saved.getTotalAmount(), saved.getFareCurrency(),
                    "Booking: " + saved.getBookingReference(), saved.getBookingReference(), userId);
            bookingSagaService.recordPaymentInitiated(saga.getId(), paymentInit);
            log.info("Booking {} created for user {}, payment initiated for {}", saved.getBookingReference(), userId, saved.getTotalAmount());
            return paymentInit;
        } catch (RuntimeException e) {
            log.error("createBooking failed for Idempotency-Key {}, compensating", key, e);
            compensate(saved, saved.getBookingReference(), saved.getSeatInstanceIds(), saga, e);
            throw e;
        }
    }

    private BookingSaga beginSaga(BookingRequest request, UUID userId, String key) {
        String requestHash = bookingSagaService.hashRequest(request, userId);
        BookingSaga saga = bookingSagaService.tryStart(userId, key, requestHash);
        if (!saga.getRequestHash().equals(requestHash)) {
            throw new ConflictException(String.format(ErrorMessageUtil.IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST, key));
        }
        return saga;
    }

    private static boolean paymentInitiated(BookingSaga saga) {
        return saga.getResponseJson() != null
                && (saga.getStatus() == SagaStatus.PAYMENT_INITIATED || saga.getStatus() == SagaStatus.COMPLETED);
    }

    private static void requireStarted(BookingSaga saga, String key) {
        if (saga.getStatus() != SagaStatus.STARTED) {
            throw new ConflictException(String.format(ErrorMessageUtil.IDEMPOTENCY_REQUEST_IN_PROGRESS, key));
        }
    }

    /**
     * Prices one flight, holds its seats and saves the PENDING booking; no payment. If anything fails it undoes its
     * own work (seats released, booking cancelled, saga failed) before rethrowing.
     */
    private Booking reserve(BookingRequest request, UUID userId, BookingSaga saga, Booking exchangedFrom, UUID tripId) {
        Booking saved = null;
        String bookingReference = null;
        List<UUID> heldSeatIds = null;
        try {
            if (request.getTripType() != TripType.ONE_WAY && tripId == null) {
                throw new BadRequestException(ErrorMessageUtil.ONLY_ONE_WAY_BOOKINGS_SUPPORTED);
            }
            List<UUID> seatInstanceIds = requireDistinctSeats(request.getPassengers());

            Pricing pricing = price(request, exchangedFrom);
            UUID flightId = pricing.flightId();
            LocalDate departureDate = pricing.departureDate();
            FlightResponse flightResponse = pricing.flight();
            Map<PassengerType, FareQuoteResponse> quotes = pricing.quotes();
            FareQuoteResponse quote = pricing.adultQuote();
            BigDecimal ancillaryPrice = pricing.ancillaryPrice();
            BigDecimal mealPrice = pricing.mealPrice();

            bookingReference = generateBookingReference();
            SeatHoldResponse hold = seatClient.holdSeats(SeatHoldRequest.builder()
                    .flightInstanceId(request.getFlightInstanceId())
                    .cabinClass(request.getCabinClass())
                    .seatInstanceIds(seatInstanceIds)
                    .bookingReference(bookingReference)
                    .build());
            heldSeatIds = seatInstanceIds;
            bookingSagaService.recordSeatsHeld(saga.getId(), bookingReference, seatInstanceIds);

            Set<Passenger> passengers = new HashSet<>();
            for (PassengerRequest passengerRequest : request.getPassengers()) {
                Passenger passenger = PassengerMapper.toEntity(passengerRequest);
                passenger.setPrimaryUserId(userId);
                PassengerType type = Passenger.typeOn(passenger.getDateOfBirth(), departureDate);
                FareQuoteResponse passengerQuote = quotes.get(Passenger.pricingType(type, passenger.getSeatInstanceId()));
                passenger.setPassengerType(type);
                passenger.setFareId(passengerQuote.getFareId());
                passenger.setFareAmount(passengerQuote.getTotalPrice());
                passenger.setFareCancellationFee(passengerQuote.getCancellationFee());
                passenger.setFareChangeFee(passengerQuote.getChangeFee());
                passengers.add(passenger);
            }

            Booking booking = BookingMapper.toEntity(request, userId, passengers, bookingReference);
            booking.setFlightId(flightId);
            booking.setStatus(BookingStatus.PENDING);
            booking.setAirlineId(flightResponse.getAirline().getId());
            booking.setSeatInstanceIds(new ArrayList<>(seatInstanceIds));
            booking.setTripId(tripId);
            booking.setTicketTimeLimit(LocalDateTime.ofInstant(hold.getHoldExpiresAt(), ZoneId.systemDefault()));

            booking.setFareVersion(quote.getVersion());
            booking.setFareCurrency(quote.getCurrency());
            booking.setFareBaseAmount(quote.getBaseFare());
            booking.setFareTaxAmount(quote.getTaxesAndFees());
            booking.setFareFeeAmount(quote.getAirlineFees());
            booking.setFareRulesId(quote.getFareRulesId());
            booking.setFareBaggagePolicyId(quote.getBaggagePolicyId());
            booking.setFareTravelDate(departureDate);
            booking.setFareRefundable(Boolean.TRUE.equals(quote.getRefundable()));
            booking.setFareCancellationFee(quote.getCancellationFee());
            booking.setFareRefundDeadlineDays(quote.getRefundDeadlineDays());
            booking.setFareChangeable(Boolean.TRUE.equals(quote.getChangeable()));
            booking.setFareChangeFee(quote.getChangeFee());
            booking.setFareChangeDeadlineHours(quote.getChangeDeadlineHours());
            if (exchangedFrom != null) {
                booking.setExchangedFromBookingId(exchangedFrom.getId());
                if (!Boolean.TRUE.equals(exchangedFrom.getFareRefundable())) {
                    booking.setFareRefundable(false);
                }
            }

            BigDecimal fareTotal = passengers.stream().map(Passenger::getFareAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal changeFees = exchangedFrom != null ? exchangedFrom.changeFees() : BigDecimal.ZERO;
            BigDecimal totalPrice = fareTotal.add(hold.getTotalPremium()).add(ancillaryPrice).add(mealPrice).add(changeFees)
                    .setScale(2, RoundingMode.HALF_EVEN);
            booking.setTotalAmount(totalPrice);
            if (exchangedFrom != null) {
                booking.setChangeFeeAmount(changeFees);
            }

            for (Passenger passenger : passengers) {
                passenger.setBooking(booking);
            }
            saved = bookingRepository.save(booking);
            bookingSagaService.recordBookingCreated(saga.getId(), saved.getId());
            return saved;
        } catch (RuntimeException e) {
            log.error("Reservation failed for Idempotency-Key {}, compensating", saga.getIdempotencyKey(), e);
            compensate(saved, bookingReference, heldSeatIds, saga, e);
            throw e;
        }
    }

    private void compensate(Booking saved, String bookingReference, List<UUID> heldSeatIds, BookingSaga saga, RuntimeException cause) {
        if (saved != null) {
            saved.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(saved);
        }
        if (heldSeatIds != null && !heldSeatIds.isEmpty()) {
            bookingEventProducer.sendSeatsReleased(saved != null ? saved.getId() : null, bookingReference, heldSeatIds);
        }
        bookingSagaService.fail(saga.getId(), cause.getMessage());
    }

    /** {@code payableId} is a booking, a trip or an extras order: payment-service asks booking-service what it is. */
    private PaymentInitiateResponse initiatePayment(UUID payableId, BigDecimal amount, String currency,
                                                    String description, String idempotencyKey, UUID userId) {
        PaymentInitiateResponse paymentInit = paymentClient.initiatePayment(PaymentInitiateRequest.builder()
                .userId(userId)
                .bookingId(payableId)
                .amount(amount)
                .currency(currency)
                .gateway(paymentProperties.gatewayFor(currency))
                .description(description)
                .idempotencyKey(idempotencyKey)
                .build(), userId);
        if (paymentInit == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ErrorMessageUtil.PAYMENT_SERVICE_UNAVAILABLE);
        }
        return paymentInit;
    }

    @Override
    public PaymentInitiateResponse createTrip(TripRequest request, UUID userId, String idempotencyKey) {
        requireTripShape(request);
        String key = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();
        List<BookingRequest> legs = legsOf(request);
        List<BookingSaga> sagas = new ArrayList<>();
        for (int i = 0; i < legs.size(); i++) {
            sagas.add(beginSaga(legs.get(i), userId, key + "#" + i));
        }
        if (paymentInitiated(sagas.get(0))) {
            log.info("Idempotency-Key {} already initiated the trip's payment, returning the original response", key);
            return bookingSagaService.readResponse(sagas.get(0));
        }
        sagas.forEach(saga -> requireStarted(saga, key));
        requireItinerary(request);

        Trip trip = tripRepository.save(Trip.builder()
                .tripReference(generateTripReference())
                .userId(userId)
                .tripType(request.getTripType())
                .status(BookingStatus.PENDING)
                .build());
        List<Booking> reserved = new ArrayList<>();
        try {
            for (int i = 0; i < legs.size(); i++) {
                reserved.add(reserve(legs.get(i), userId, sagas.get(i), null, trip.getId()));
            }
            String currency = reserved.get(0).getFareCurrency();
            if (reserved.stream().anyMatch(leg -> !currency.equals(leg.getFareCurrency()))) {
                throw new ConflictException(ErrorMessageUtil.TRIP_CURRENCY_MISMATCH);
            }
            trip.setCurrency(currency);
            trip.setAirlineId(reserved.get(0).getAirlineId());
            trip.setTotalAmount(reserved.stream().map(Booking::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            tripRepository.save(trip);

            PaymentInitiateResponse paymentInit = initiatePayment(trip.getId(), trip.getTotalAmount(), currency,
                    "Trip: " + trip.getTripReference(), trip.getTripReference(), userId);
            sagas.forEach(saga -> bookingSagaService.recordPaymentInitiated(saga.getId(), paymentInit));
            log.info("Trip {} of {} flights created for user {}, payment initiated for {}",
                    trip.getTripReference(), legs.size(), userId, trip.getTotalAmount());
            return paymentInit;
        } catch (RuntimeException e) {
            log.error("createTrip failed for Idempotency-Key {}, compensating", key, e);
            for (int i = 0; i < reserved.size(); i++) {
                Booking leg = reserved.get(i);
                compensate(leg, leg.getBookingReference(), leg.getSeatInstanceIds(), sagas.get(i), e);
            }
            for (int i = reserved.size() + 1; i < sagas.size(); i++) {
                bookingSagaService.fail(sagas.get(i).getId(), e.getMessage());
            }
            trip.setStatus(BookingStatus.CANCELLED);
            tripRepository.save(trip);
            throw e;
        }
    }

    @Override
    public TripResponse getTrip(UUID id, UUID userId) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.TRIP_NOT_FOUND_BY_ID, id)));
        boolean owner = trip.getUserId().equals(userId);
        if (!owner) {
            bookingAccess.requireAirlinePermission(trip.getAirlineId(), userId, BookingPermissions.BOOKING_READ);
        }
        List<Booking> legs = inTransaction(true, () -> bookingRepository.findByTripId(id).stream()
                .map(BookingServiceImpl::initialized)
                .sorted(Comparator.comparing(Booking::getFareTravelDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList());
        List<BookingResponse> bookings = enrichBatch(legs);
        if (!owner && !bookingAccess.hasAirlinePermission(trip.getAirlineId(), userId, BookingPermissions.PASSENGER_PII_READ)) {
            bookings.forEach(this::maskPii);
        }
        return TripResponse.builder()
                .id(trip.getId())
                .tripReference(trip.getTripReference())
                .tripType(trip.getTripType())
                .status(trip.getStatus())
                .currency(trip.getCurrency())
                .totalAmount(trip.getTotalAmount())
                .paymentId(trip.getPaymentId())
                .createdAt(trip.getCreatedAt())
                .bookings(bookings)
                .build();
    }

    @Override
    public TripResponse cancelTrip(UUID id, UUID userId) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.TRIP_NOT_FOUND_BY_ID, id)));
        for (Booking leg : bookingRepository.findByTripId(id)) {
            if (leg.getStatus() == BookingStatus.PENDING || leg.getStatus() == BookingStatus.CONFIRMED) {
                cancelBooking(leg.getId(), userId);
            }
        }
        trip.setStatus(BookingStatus.CANCELLED);
        tripRepository.save(trip);
        return getTrip(id, userId);
    }

    /** Round trip: exactly two flights; multi-city: two to MAX_TRIP_FLIGHTS; one seat entry per passenger per flight. */
    private static void requireTripShape(TripRequest request) {
        int flights = request.getSegments().size();
        switch (request.getTripType()) {
            case ONE_WAY -> throw new BadRequestException(ErrorMessageUtil.TRIP_ONE_WAY_NOT_A_TRIP);
            case ROUND_TRIP -> {
                if (flights != 2) {
                    throw new BadRequestException(String.format(ErrorMessageUtil.TRIP_ROUND_TRIP_NEEDS_TWO_FLIGHTS, flights));
                }
            }
            case MULTI_CITY -> {
                if (flights < 2 || flights > MAX_TRIP_FLIGHTS) {
                    throw new BadRequestException(String.format(ErrorMessageUtil.TRIP_MULTI_CITY_FLIGHT_COUNT, MAX_TRIP_FLIGHTS, flights));
                }
            }
        }
        int passengers = request.getPassengers().size();
        for (TripSegmentRequest segment : request.getSegments()) {
            int seats = segment.getSeatInstanceIds() != null ? segment.getSeatInstanceIds().size() : 0;
            if (seats != passengers) {
                throw new BadRequestException(String.format(
                        ErrorMessageUtil.TRIP_SEAT_PER_PASSENGER, segment.getFlightInstanceId(), passengers, seats));
            }
        }
    }

    /** The flights follow each other in time, belong to one airline, and a round trip comes back the way it went. */
    private void requireItinerary(TripRequest request) {
        List<FlightInstanceResponse> flights = request.getSegments().stream()
                .map(segment -> flightClient.getFlightInstanceResponse(segment.getFlightInstanceId()))
                .toList();
        for (int i = 1; i < flights.size(); i++) {
            if (!flights.get(i).getDepartureDateTime().isAfter(flights.get(i - 1).getDepartureDateTime())) {
                throw new BadRequestException(String.format(ErrorMessageUtil.TRIP_FLIGHTS_OUT_OF_ORDER, flights.get(i).getId()));
            }
        }
        if (flights.stream().map(FlightInstanceResponse::getAirlineId).distinct().count() > 1) {
            throw new BadRequestException(ErrorMessageUtil.TRIP_ONE_AIRLINE);
        }
        if (request.getTripType() == TripType.ROUND_TRIP) {
            FlightInstanceResponse out = flights.get(0);
            FlightInstanceResponse back = flights.get(1);
            if (!sameAirport(back.getDepartureAirport(), out.getArrivalAirport())
                    || !sameAirport(back.getArrivalAirport(), out.getDepartureAirport())) {
                throw new BadRequestException(ErrorMessageUtil.TRIP_NOT_A_ROUND_TRIP);
            }
        }
    }

    private static boolean sameAirport(AirportResponse a, AirportResponse b) {
        return a != null && b != null && a.getId() != null && a.getId().equals(b.getId());
    }

    /** One one-way booking request per flight, with the trip's passengers on that flight's seats. */
    private static List<BookingRequest> legsOf(TripRequest request) {
        List<BookingRequest> legs = new ArrayList<>();
        for (TripSegmentRequest segment : request.getSegments()) {
            List<PassengerRequest> passengers = new ArrayList<>();
            for (int p = 0; p < request.getPassengers().size(); p++) {
                passengers.add(PassengerMapper.withSeat(request.getPassengers().get(p), segment.getSeatInstanceIds().get(p)));
            }
            legs.add(BookingRequest.builder()
                    .flightInstanceId(segment.getFlightInstanceId())
                    .fareId(segment.getFareId())
                    .cabinClass(segment.getCabinClass())
                    .tripType(request.getTripType())
                    .contactInfo(request.getContactInfo())
                    .passengers(passengers)
                    .ancillaryIds(segment.getAncillaryIds())
                    .mealIds(segment.getMealIds())
                    .build());
        }
        return legs;
    }

    private String generateTripReference() {
        String reference;
        do {
            reference = "TR" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (tripRepository.existsByTripReference(reference));
        return reference;
    }

    @Override
    public BookingResponse updateBooking(UUID id, BookingRequest request, UUID userId) {
        bookingAccess.requireOwnerOrPermission(inTransaction(true, () -> findBookingOrThrow(id)), userId, BookingPermissions.BOOKING_MANAGE);
        Booking updated = inTransaction(false, () -> applyDetailsUpdate(id, request));
        return convertBookingResponse(updated, canSeePii(updated, userId));
    }

    private Booking applyDetailsUpdate(UUID id, BookingRequest request) {
        Booking booking = findBookingOrThrow(id);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ConflictException(String.format(
                    ErrorMessageUtil.BOOKING_NOT_MODIFIABLE, booking.getBookingReference(), booking.getStatus()));
        }
        requireDetailsOnlyChange(booking, request);

        Map<UUID, Passenger> passengersBySeat = booking.getPassengers().stream()
                .filter(passenger -> passenger.getSeatInstanceId() != null)
                .collect(Collectors.toMap(Passenger::getSeatInstanceId, Function.identity()));
        Map<UUID, Passenger> passengersById = booking.getPassengers().stream()
                .collect(Collectors.toMap(Passenger::getId, Function.identity()));
        for (PassengerRequest passengerRequest : request.getPassengers()) {
            Passenger passenger = storedPassenger(booking, passengerRequest, passengersById, passengersBySeat);
            requireSamePassengerType(passenger, passengerRequest, booking.getFareTravelDate());
            PassengerMapper.updateEntityFromRequest(passengerRequest, passenger);
        }
        if (request.getContactInfo() != null) {
            booking.setContactInfo(request.getContactInfo());
        }
        return initialized(bookingRepository.save(booking));
    }

    @Override
    public BookingResponse getBookingById(UUID id, UUID userId) {
        Booking booking = inTransaction(true, () -> bookingRepository.findByIdWithDetails(id).map(BookingServiceImpl::initialized))
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.BOOKING_NOT_FOUND_BY_ID, id)));
        bookingAccess.requireOwnerOrPermission(booking, userId, BookingPermissions.BOOKING_READ);
        return convertBookingResponse(booking, canSeePii(booking, userId));
    }

    @Override
    public BookingResponse getBookingForSupport(String bookingReference) {
        Booking booking = inTransaction(true, () -> bookingRepository.findByBookingReference(bookingReference).map(BookingServiceImpl::initialized))
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.BOOKING_NOT_FOUND_BY_REFERENCE, bookingReference)));
        return convertBookingResponse(booking, false);
    }

    @Override
    public Page<BookingResponse> getBookingsByAirline(
            UUID userId,
            UUID airlineId,
            String searchQuery,
            BookingStatus status,
            UUID flightInstanceId,
            String sortDirection,
            int page,
            int size
    ) {
        bookingAccess.requireAirlinePermission(airlineId, userId, BookingPermissions.BOOKING_READ);
        boolean seePii = bookingAccess.hasAirlinePermission(airlineId, userId, BookingPermissions.PASSENGER_PII_READ);

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String search = searchQuery == null || searchQuery.isBlank() ? null : searchQuery.trim();
        Page<BookingResponse> responses = loadPage(bookingRepository.findIdsByAirlineWithFilters(
                airlineId, search, status, flightInstanceId, pageRequest(page, size, direction)));
        if (!seePii) {
            responses.forEach(this::maskPii);
        }
        return responses;
    }

    @Override
    public Page<BookingResponse> getBookingsByUser(UUID userId, int page, int size) {
        return loadPage(bookingRepository.findIdsByUserId(userId, pageRequest(page, size, Sort.Direction.DESC)));
    }

    @Override
    public List<PassengerResponse> getPassengersByBooking(UUID bookingId, UUID userId) {
        Booking booking = inTransaction(true, () -> initialized(findBookingOrThrow(bookingId)));
        bookingAccess.requireOwnerOrPermission(booking, userId, BookingPermissions.PASSENGER_READ);

        List<PassengerResponse> passengers = booking.getPassengers().stream()
                .map(PassengerMapper::toResponse)
                .collect(Collectors.toList());
        if (!canSeePii(booking, userId)) {
            passengers.forEach(BookingServiceImpl::maskPii);
        }
        return passengers;
    }

    @Override
    public BookingResponse cancelBooking(UUID id, UUID userId) {
        bookingAccess.requireOwnerOrPermission(inTransaction(true, () -> findBookingOrThrow(id)), userId, BookingPermissions.BOOKING_MANAGE);
        Booking updated = inTransaction(false, () -> cancel(id, userId));
        return convertBookingResponse(updated, canSeePii(updated, userId));
    }

    /** The refund is the one remote call inside the transaction, on purpose: nothing changes unless it succeeds. */
    private Booking cancel(UUID id, UUID userId) {
        Booking booking = findBookingOrThrow(id);
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException(String.format(
                    ErrorMessageUtil.BOOKING_NOT_CANCELLABLE, booking.getBookingReference(), booking.getStatus()));
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            BigDecimal refund = refundDue(booking, LocalDate.now(clock));
            ticketService.closeTicketsForBooking(booking, refund.signum() > 0 ? TicketStatus.REFUNDED : TicketStatus.CANCELLED);
            if (refund.signum() > 0) {
                if (booking.getPaymentId() == null) {
                    throw new ConflictException(String.format(ErrorMessageUtil.BOOKING_NO_PAYMENT_TO_REFUND, booking.getBookingReference()));
                }
                paymentClient.refundPaymentAsSystem(booking.getPaymentId(), refund, "cancel:" + booking.getId());
                booking.setRefundAmount(refund);
            }
            extrasOrderService.closeOrders(booking, refund.signum() > 0);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking updated = bookingRepository.save(booking);
        bookingEventProducer.sendBookingCancelled(updated, ErrorMessageUtil.BOOKING_CANCELLED_BY_REQUEST);
        bookingSagaService.cancel(updated.getId());
        log.info("Booking {} cancelled by {}", updated.getBookingReference(), userId);
        return initialized(updated);
    }

    @Override
    public long countByFlightId(UUID userId, UUID airlineId, UUID flightInstanceId) {
        bookingAccess.requireAirlinePermission(airlineId, userId, BookingPermissions.BOOKING_READ);
        return bookingRepository.countByFlightInstanceIdAndAirlineId(flightInstanceId, airlineId);
    }

    @Override
    public BookingStatisticsResponse getBookingStatisticsForAirline(UUID userId, UUID airlineId) {
        bookingAccess.requireAirlinePermission(airlineId, userId, BookingPermissions.BOOKING_READ);

        LocalDate today = LocalDate.now(clock);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        LocalDateTime startOfMonth = YearMonth.from(today).atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = YearMonth.from(today).atEndOfMonth().atTime(LocalTime.MAX);

        Map<String, BigDecimal> revenueToday = revenueByCurrency(airlineId, startOfDay, endOfDay);
        Map<String, BigDecimal> revenueThisMonth = revenueByCurrency(airlineId, startOfMonth, endOfMonth);
        String currency = revenueThisMonth.size() == 1 ? revenueThisMonth.keySet().iterator().next() : null;
        boolean singleCurrency = revenueThisMonth.size() <= 1;
        LocalDateTime trendFrom = YearMonth.from(today).minusMonths(TREND_MONTHS - 1).atDay(1).atStartOfDay();
        List<DailyTotal> trend = new ArrayList<>(bookingRepository.sumSoldBookingsByDayAndCurrency(airlineId, trendFrom));
        extrasOrderRepository.sumConfirmedByDayAndCurrency(airlineId, trendFrom)
                .forEach(row -> trend.add(new ExtrasRevenue(row.getBookingDay(), row.getCurrency(), 0L, row.getTotal())));

        return BookingStatisticsResponse.builder()
                .totalBookingsToday(bookingRepository.countByAirlineIdAndBookingDateBetween(airlineId, startOfDay, endOfDay))
                .revenueToday(singleCurrency ? revenueToday.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add) : null)
                .revenueTodayByCurrency(revenueToday)
                .totalBookingsThisMonth(bookingRepository.countByAirlineIdAndBookingDateBetween(airlineId, startOfMonth, endOfMonth))
                .revenueThisMonth(singleCurrency ? revenueThisMonth.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add) : null)
                .revenueThisMonthByCurrency(revenueThisMonth)
                .currency(currency)
                .dailyTrend(dailyTrend(trend, today))
                .monthlyData(monthlyTrend(trend, YearMonth.from(today)))
                .build();
    }

    /** Extras revenue in a trend: it adds to a day's revenue but not to its booking count. */
    private record ExtrasRevenue(LocalDate getBookingDay, String getCurrency, Long getBookings, BigDecimal getTotal) implements DailyTotal {}

    /** Sold bookings and revenue (bookings and confirmed extras) per day for the last 30 days, with empty days included. */
    private static List<BookingStatisticsResponse.DailyBookingData> dailyTrend(List<DailyTotal> trend, LocalDate today) {
        boolean singleCurrency = trend.stream().map(DailyTotal::getCurrency).distinct().count() <= 1;
        Map<LocalDate, List<DailyTotal>> byDay = trend.stream().collect(Collectors.groupingBy(DailyTotal::getBookingDay));
        List<BookingStatisticsResponse.DailyBookingData> days = new ArrayList<>();
        for (LocalDate day = today.minusDays(TREND_DAYS - 1); !day.isAfter(today); day = day.plusDays(1)) {
            List<DailyTotal> rows = byDay.getOrDefault(day, List.of());
            Map<String, BigDecimal> revenue = revenueByCurrency(rows);
            days.add(BookingStatisticsResponse.DailyBookingData.builder()
                    .date(day.toString())
                    .bookingCount(rows.stream().mapToLong(DailyTotal::getBookings).sum())
                    .revenue(singleCurrency ? sum(revenue) : null)
                    .revenueByCurrency(revenue)
                    .build());
        }
        return days;
    }

    /** The same per month for the last 12 months. */
    private static List<BookingStatisticsResponse.MonthlyData> monthlyTrend(List<DailyTotal> trend, YearMonth thisMonth) {
        boolean singleCurrency = trend.stream().map(DailyTotal::getCurrency).distinct().count() <= 1;
        Map<YearMonth, List<DailyTotal>> byMonth = trend.stream()
                .collect(Collectors.groupingBy(row -> YearMonth.from(row.getBookingDay())));
        List<BookingStatisticsResponse.MonthlyData> months = new ArrayList<>();
        for (YearMonth month = thisMonth.minusMonths(TREND_MONTHS - 1); !month.isAfter(thisMonth); month = month.plusMonths(1)) {
            List<DailyTotal> rows = byMonth.getOrDefault(month, List.of());
            Map<String, BigDecimal> revenue = revenueByCurrency(rows);
            months.add(BookingStatisticsResponse.MonthlyData.builder()
                    .month(month.toString())
                    .bookingCount(rows.stream().mapToLong(DailyTotal::getBookings).sum())
                    .revenue(singleCurrency ? sum(revenue) : null)
                    .revenueByCurrency(revenue)
                    .build());
        }
        return months;
    }

    private static Map<String, BigDecimal> revenueByCurrency(List<DailyTotal> rows) {
        return rows.stream().collect(Collectors.toMap(DailyTotal::getCurrency, DailyTotal::getTotal, BigDecimal::add, TreeMap::new));
    }

    private static BigDecimal sum(Map<String, BigDecimal> amounts) {
        return amounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public PayableBookingResponse getPayableBooking(UUID id) {
        Booking booking = bookingRepository.findById(id).orElse(null);
        if (booking == null) {
            return tripRepository.findById(id)
                    .map(trip -> PayableBookingResponse.builder()
                            .bookingId(trip.getId())
                            .userId(trip.getUserId())
                            .airlineId(trip.getAirlineId())
                            .totalAmount(trip.getTotalAmount())
                            .currency(trip.getCurrency())
                            .status(trip.getStatus())
                            .build())
                    .or(() -> extrasOrderRepository.findById(id).map(order -> PayableBookingResponse.builder()
                            .bookingId(order.getId())
                            .userId(order.getUserId())
                            .airlineId(order.getAirlineId())
                            .totalAmount(order.getTotalAmount())
                            .currency(order.getCurrency())
                            .status(order.getStatus())
                            .build()))
                    .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.PAYABLE_NOT_FOUND, id)));
        }
        return PayableBookingResponse.builder()
                .bookingId(booking.getId())
                .userId(booking.getUserId())
                .airlineId(booking.getAirlineId())
                .totalAmount(booking.getTotalAmount())
                .currency(booking.getFareCurrency())
                .status(booking.getStatus())
                .build();
    }

    private Booking findBookingOrThrow(UUID id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.BOOKING_NOT_FOUND_BY_ID, id)));
    }

    static BigDecimal refundDue(Booking booking, LocalDate today) {
        if (!Boolean.TRUE.equals(booking.getFareRefundable()) || booking.getTotalAmount() == null) {
            return BigDecimal.ZERO;
        }
        LocalDate travelDate = booking.getFareTravelDate();
        int deadlineDays = booking.getFareRefundDeadlineDays() != null ? booking.getFareRefundDeadlineDays() : 0;
        if (travelDate != null && ChronoUnit.DAYS.between(today, travelDate) < deadlineDays) {
            return BigDecimal.ZERO;
        }
        BigDecimal fees = booking.getPassengers().stream()
                .map(passenger -> cancellationFee(booking, passenger))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refund = booking.refundableValue().subtract(fees);
        return refund.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Sold bookings and confirmed extras orders, per currency. */
    private Map<String, BigDecimal> revenueByCurrency(UUID airlineId, LocalDateTime from, LocalDateTime to) {
        return Stream.concat(bookingRepository.sumSoldRevenueByCurrency(airlineId, from, to).stream(),
                        extrasOrderRepository.sumConfirmedRevenueByCurrency(airlineId, from, to).stream())
                .collect(Collectors.toMap(CurrencyTotal::getCurrency, CurrencyTotal::getTotal, BigDecimal::add, TreeMap::new));
    }

    private static PageRequest pageRequest(int page, int size, Sort.Direction direction) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by(direction, "bookingDate"));
    }

    private Page<BookingResponse> loadPage(Page<UUID> ids) {
        Map<UUID, Booking> byId = byId(inTransaction(true, () -> bookingRepository.findWithDetailsByIdIn(ids.getContent()).stream()
                .map(BookingServiceImpl::initialized).toList()), Booking::getId);
        List<Booking> bookings = ids.getContent().stream().map(byId::get).filter(Objects::nonNull).toList();
        return new PageImpl<>(enrichBatch(bookings), ids.getPageable(), ids.getTotalElements());
    }

    private static List<UUID> requireDistinctSeats(List<PassengerRequest> passengers) {
        List<UUID> seatInstanceIds = passengers.stream().map(PassengerRequest::getSeatInstanceId).filter(Objects::nonNull).toList();
        Set<UUID> seen = new HashSet<>();
        for (UUID seatInstanceId : seatInstanceIds) {
            if (!seen.add(seatInstanceId)) {
                throw new BadRequestException(String.format(ErrorMessageUtil.DUPLICATE_SEAT_IN_BOOKING, seatInstanceId));
            }
        }
        return seatInstanceIds;
    }

    /** Each passenger's own fare's fee; bookings priced before per-passenger fares fall back to the booking's. */
    private static BigDecimal cancellationFee(Booking booking, Passenger passenger) {
        BigDecimal fee = passenger.getFareId() != null ? passenger.getFareCancellationFee() : booking.getFareCancellationFee();
        return fee != null ? fee : BigDecimal.ZERO;
    }

    /**
     * One quote per fare type on the booking, adult first: the adult quote is the booking's fare snapshot. Everyone
     * but an infant needs a seat; an infant with a seat is priced as a child, one without is a lap infant and needs
     * an adult's lap. Every fare must be in the adult fare's currency.
     */
    private Map<PassengerType, FareQuoteResponse> quoteByPassengerType(BookingRequest request, UUID flightId, LocalDate departureDate) {
        long adults = 0;
        long lapInfants = 0;
        Set<PassengerType> pricingTypes = EnumSet.noneOf(PassengerType.class);
        for (PassengerRequest passenger : request.getPassengers()) {
            PassengerType type = Passenger.typeOn(passenger.getDateOfBirth(), departureDate);
            if (passenger.getSeatInstanceId() == null && type != PassengerType.INF) {
                throw new BadRequestException(String.format(
                        ErrorMessageUtil.PASSENGER_SEAT_REQUIRED, passenger.getFirstName(), passenger.getLastName()));
            }
            if (type == PassengerType.ADT) adults++;
            if (passenger.getSeatInstanceId() == null) lapInfants++;
            pricingTypes.add(Passenger.pricingType(type, passenger.getSeatInstanceId()));
        }
        if (adults == 0) {
            throw new BadRequestException(ErrorMessageUtil.BOOKING_NEEDS_ADULT);
        }
        if (lapInfants > adults) {
            throw new BadRequestException(String.format(ErrorMessageUtil.BOOKING_TOO_MANY_INFANTS, lapInfants, adults));
        }

        Map<PassengerType, FareQuoteResponse> quotes = new EnumMap<>(PassengerType.class);
        for (PassengerType type : pricingTypes) {
            quotes.put(type, pricingIntegrationService.quoteFare(
                    request.getFareId(), flightId, request.getCabinClass(), type, departureDate));
        }
        String currency = quotes.get(PassengerType.ADT).getCurrency();
        quotes.forEach((type, quote) -> {
            if (!currency.equals(quote.getCurrency())) {
                throw new ConflictException(String.format(
                        ErrorMessageUtil.BOOKING_FARE_CURRENCY_MISMATCH, type, quote.getCurrency(), currency));
            }
        });
        return quotes;
    }

    /** A passenger was priced for their age band; a date-of-birth correction must not move them to another one. */
    private static void requireSamePassengerType(Passenger passenger, PassengerRequest request, LocalDate travelDate) {
        if (passenger.getPassengerType() == null || travelDate == null) {
            return;
        }
        PassengerType corrected = Passenger.typeOn(request.getDateOfBirth(), travelDate);
        if (corrected != passenger.getPassengerType()) {
            throw new ConflictException(String.format(
                    ErrorMessageUtil.PASSENGER_TYPE_CHANGE_NOT_ALLOWED, passenger.getPassengerType(), corrected));
        }
    }

    private static Passenger storedPassenger(Booking booking, PassengerRequest request,
                                             Map<UUID, Passenger> byId, Map<UUID, Passenger> bySeat) {
        if (request.getId() != null) {
            Passenger passenger = byId.get(request.getId());
            if (passenger == null) {
                throw new BadRequestException(String.format(
                        ErrorMessageUtil.PASSENGER_NOT_IN_BOOKING, request.getId(), booking.getBookingReference()));
            }
            return passenger;
        }
        if (request.getSeatInstanceId() == null) {
            throw new BadRequestException(ErrorMessageUtil.LAP_INFANT_NEEDS_PASSENGER_ID);
        }
        return bySeat.get(request.getSeatInstanceId());
    }

    /** The fare's change rules, as snapshotted at purchase, and no other change already waiting for payment. */
    private void requireChangeable(Booking booking) {
        String reference = booking.getBookingReference();
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException(String.format(ErrorMessageUtil.BOOKING_NOT_CHANGEABLE_STATUS, reference, booking.getStatus()));
        }
        if (booking.getTickets().stream().anyMatch(ticket -> ticket.getStatus() == TicketStatus.USED)) {
            throw new ConflictException(String.format(ErrorMessageUtil.BOOKING_HAS_USED_TICKETS, reference));
        }
        if (!Boolean.TRUE.equals(booking.getFareChangeable())) {
            throw new ConflictException(String.format(ErrorMessageUtil.BOOKING_FARE_NOT_CHANGEABLE, reference));
        }
        if (bookingRepository.existsByExchangedFromBookingIdAndStatus(booking.getId(), BookingStatus.PENDING)) {
            throw new ConflictException(String.format(ErrorMessageUtil.BOOKING_CHANGE_PENDING, reference));
        }
        FlightInstanceResponse departure = flightClient.getFlightInstanceResponse(booking.getFlightInstanceId());
        if (departure == null || departure.getDepartureDateTime() == null) {
            throw new ServiceUnavailableException(ErrorMessageUtil.FLIGHT_SERVICE_UNAVAILABLE);
        }
        int deadlineHours = booking.getFareChangeDeadlineHours() != null ? booking.getFareChangeDeadlineHours() : 0;
        if (LocalDateTime.now(clock).isAfter(departure.getDepartureDateTime().minusHours(deadlineHours))) {
            throw new ConflictException(String.format(ErrorMessageUtil.BOOKING_CHANGE_DEADLINE_PASSED, reference, deadlineHours));
        }
    }

    /** The original's passengers and contact details on the new departure, with the requested seats and extras. */
    private static BookingRequest rebookingRequest(Booking original, BookingChangeRequest request) {
        Set<UUID> passengerIds = original.getPassengers().stream().map(Passenger::getId).collect(Collectors.toSet());
        Map<UUID, UUID> seatByPassenger = new HashMap<>();
        for (BookingChangeRequest.SeatAssignment seat : request.getSeats() != null ? request.getSeats() : List.<BookingChangeRequest.SeatAssignment>of()) {
            if (!passengerIds.contains(seat.getPassengerId())) {
                throw new BadRequestException(String.format(
                        ErrorMessageUtil.PASSENGER_NOT_IN_BOOKING, seat.getPassengerId(), original.getBookingReference()));
            }
            seatByPassenger.put(seat.getPassengerId(), seat.getSeatInstanceId());
        }
        List<PassengerRequest> passengers = original.getPassengers().stream()
                .sorted(Comparator.comparing(Passenger::getId))
                .map(passenger -> PassengerMapper.toRequest(passenger, seatByPassenger.get(passenger.getId())))
                .toList();
        return BookingRequest.builder()
                .flightInstanceId(request.getFlightInstanceId())
                .fareId(request.getFareId())
                .cabinClass(request.getCabinClass() != null ? request.getCabinClass() : original.getCabinClass())
                .tripType(TripType.ONE_WAY)
                .contactInfo(original.getContactInfo())
                .passengers(passengers)
                .ancillaryIds(request.getAncillaryIds())
                .mealIds(request.getMealIds())
                .build();
    }

    /** A change can go to any route the airline flies, but not to another airline: the fare terms and payment are its. */
    private static void requireSameAirline(Booking original, FlightResponse replacement) {
        if (replacement.getAirline() == null || !original.getAirlineId().equals(replacement.getAirline().getId())) {
            throw new BadRequestException(String.format(ErrorMessageUtil.BOOKING_CHANGE_OTHER_AIRLINE, original.getBookingReference()));
        }
    }

    private static void requireDetailsOnlyChange(Booking booking, BookingRequest request) {
        Set<UUID> requestedSeats = new HashSet<>(requireDistinctSeats(request.getPassengers()));
        boolean unchanged = Objects.equals(booking.getFlightInstanceId(), request.getFlightInstanceId())
                && (request.getFlightId() == null || Objects.equals(booking.getFlightId(), request.getFlightId()))
                && Objects.equals(booking.getFareId(), request.getFareId())
                && booking.getCabinClass() == request.getCabinClass()
                && booking.getTripType() == request.getTripType()
                && sameItems(booking.getAncillaryIds(), request.getAncillaryIds())
                && sameItems(booking.getMealIds(), request.getMealIds())
                && requestedSeats.equals(new HashSet<>(booking.getSeatInstanceIds()));
        if (!unchanged) {
            throw new BadRequestException(ErrorMessageUtil.BOOKING_UPDATE_DETAILS_ONLY);
        }
    }

    private static boolean sameItems(List<UUID> current, List<UUID> requested) {
        List<UUID> a = current == null ? new ArrayList<>() : new ArrayList<>(current);
        List<UUID> b = requested == null ? new ArrayList<>() : new ArrayList<>(requested);
        a.sort(null);
        b.sort(null);
        return a.equals(b);
    }

    private boolean canSeePii(Booking booking, UUID userId) {
        return bookingAccess.isOwner(booking, userId)
                || bookingAccess.hasAirlinePermission(booking.getAirlineId(), userId, BookingPermissions.PASSENGER_PII_READ);
    }

    private void maskPii(BookingResponse response) {
        if (response.getPassengers() != null) {
            response.getPassengers().forEach(BookingServiceImpl::maskPii);
        }
    }

    private static void maskPii(PassengerResponse passenger) {
        passenger.setPassportNumber(null);
        passenger.setDateOfBirth(null);
    }

    private static void requireAirlineAcceptingBookings(FlightResponse flight) {
        AirlineResponse airline = flight.getAirline();
        if (airline == null || airline.getStatus() == null) {
            throw new ServiceUnavailableException(String.format(ErrorMessageUtil.AIRLINE_OF_FLIGHT_UNAVAILABLE, flight.getId()));
        }
        if (airline.getStatus() != AirlineStatus.ACTIVE) {
            throw new ConflictException(String.format(
                    ErrorMessageUtil.AIRLINE_NOT_ACCEPTING_BOOKINGS, airline.getId(), airline.getStatus()));
        }
    }

    private String generateBookingReference() {
        String reference;
        do {
            reference = "BK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (bookingRepository.existsByBookingReference(reference));
        return reference;
    }

    private List<BookingResponse> enrichBatch(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return new ArrayList<>();
        }

        List<UUID> bookingIds = bookings.stream().map(Booking::getId).toList();
        List<UUID> fareIds = distinct(bookings.stream().map(Booking::getFareId));
        List<UUID> flightIds = distinct(bookings.stream().map(Booking::getFlightId));
        List<UUID> flightInstanceIds = distinct(bookings.stream().map(Booking::getFlightInstanceId));
        List<UUID> seatIds = distinct(bookings.stream().flatMap(b -> stream(b.getSeatInstanceIds())));
        List<UUID> ancillaryIds = distinct(bookings.stream().flatMap(b -> stream(b.getAncillaryIds())));
        List<UUID> mealIds = distinct(bookings.stream().flatMap(b -> stream(b.getMealIds())));

        Map<UUID, FareResponse> fareMap = fareIds.isEmpty() ? Map.of() : pricingClient.getFaresByIds(fareIds);
        Map<UUID, FlightResponse> flightMap = flightIds.isEmpty() ? Map.of() : flightClient.getFlightsByIds(flightIds);
        Map<UUID, FlightInstanceResponse> flightInstanceMap = flightInstanceIds.isEmpty()
                ? Map.of() : flightClient.getFlightInstancesByIds(flightInstanceIds);
        Map<UUID, PaymentDTO> paymentMap = paymentClient.getPaymentsByBookingIds(bookingIds);
        Map<UUID, SeatInstanceResponse> seatMap = seatIds.isEmpty()
                ? Map.of() : byId(seatClient.getAllByIds(seatIds), SeatInstanceResponse::getId);
        Map<UUID, FlightCabinAncillaryResponse> ancillaryMap = ancillaryIds.isEmpty()
                ? Map.of() : byId(ancillaryClient.getAllByIds(ancillaryIds), FlightCabinAncillaryResponse::getId);
        Map<UUID, FlightMealResponse> mealMap = mealIds.isEmpty()
                ? Map.of() : byId(ancillaryClient.getMealsByIds(mealIds), FlightMealResponse::getId);

        Map<UUID, List<ExtrasOrderResponse>> extrasByBooking = extrasOrderRepository.findByBookingIdIn(bookingIds).stream()
                .collect(Collectors.groupingBy(ExtrasOrder::getBookingId,
                        Collectors.mapping(order -> ExtrasOrderMapper.toResponse(order, null), Collectors.toList())));

        return bookings.stream()
                .map(booking -> BookingMapper.toResponse(
                        booking,
                        paymentMap.get(booking.getId()),
                        lookup(fareMap, booking.getFareId()),
                        lookup(flightMap, booking.getFlightId()),
                        lookup(flightInstanceMap, booking.getFlightInstanceId()),
                        resolve(booking.getAncillaryIds(), ancillaryMap),
                        resolve(booking.getMealIds(), mealMap),
                        resolve(booking.getSeatInstanceIds(), seatMap)))
                .peek(response -> response.setExtrasOrders(extrasByBooking.getOrDefault(response.getId(), List.of())))
                .collect(Collectors.toList());
    }

    private BookingResponse convertBookingResponse(Booking booking, boolean seePii) {
        List<UUID> ancillaryIds = booking.getAncillaryIds() != null ? booking.getAncillaryIds() : List.of();
        List<UUID> mealIds = booking.getMealIds() != null ? booking.getMealIds() : List.of();
        List<UUID> seatIds = booking.getSeatInstanceIds() != null ? booking.getSeatInstanceIds() : List.of();

        Map<UUID, FlightCabinAncillaryResponse> ancillaryMap = ancillaryIds.isEmpty()
                ? Map.of() : byId(ancillaryClient.getAllByIds(ancillaryIds), FlightCabinAncillaryResponse::getId);
        Map<UUID, FlightMealResponse> mealMap = mealIds.isEmpty()
                ? Map.of() : byId(ancillaryClient.getMealsByIds(mealIds), FlightMealResponse::getId);
        Map<UUID, SeatInstanceResponse> seatMap = seatIds.isEmpty()
                ? Map.of() : byId(seatClient.getAllByIds(seatIds), SeatInstanceResponse::getId);

        BookingResponse response = BookingMapper.toResponse(booking,
                paymentClient.getPaymentByBookingId(booking.getId()),
                booking.getFareId() != null ? pricingClient.getFareById(booking.getFareId()) : null,
                booking.getFlightId() != null ? flightClient.getFlightById(booking.getFlightId()) : null,
                booking.getFlightInstanceId() != null ? flightClient.getFlightInstanceResponse(booking.getFlightInstanceId()) : null,
                resolve(ancillaryIds, ancillaryMap),
                resolve(mealIds, mealMap),
                resolve(seatIds, seatMap));
        response.setExtrasOrders(extrasOrderRepository.findByBookingIdIn(List.of(booking.getId())).stream()
                .map(order -> ExtrasOrderMapper.toResponse(order, null)).toList());
        if (!seePii) {
            maskPii(response);
        }
        return response;
    }

    /** Database work in its own transaction, so no connection is held while other services are called. */
    private <T> T inTransaction(boolean readOnly, Supplier<T> work) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setReadOnly(readOnly);
        return template.execute(status -> work.get());
    }

    /** Loads the lazy collections a response maps while the transaction is still open. */
    private static Booking initialized(Booking booking) {
        Hibernate.initialize(booking.getPassengers());
        Hibernate.initialize(booking.getTickets());
        Hibernate.initialize(booking.getSeatInstanceIds());
        Hibernate.initialize(booking.getAncillaryIds());
        Hibernate.initialize(booking.getMealIds());
        return booking;
    }

    private static <T> Stream<T> stream(List<T> values) {
        return values != null ? values.stream() : Stream.empty();
    }

    private static List<UUID> distinct(Stream<UUID> ids) {
        return ids.filter(Objects::nonNull).distinct().toList();
    }

    private static <T> Map<UUID, T> byId(List<T> values, Function<T, UUID> id) {
        return values.stream().collect(Collectors.toMap(id, Function.identity(), (a, b) -> a));
    }

    private static <T> T lookup(Map<UUID, T> values, UUID id) {
        return id != null ? values.get(id) : null;
    }

    private static <T> List<T> resolve(List<UUID> ids, Map<UUID, T> values) {
        return stream(ids).map(values::get).filter(Objects::nonNull).toList();
    }
}
