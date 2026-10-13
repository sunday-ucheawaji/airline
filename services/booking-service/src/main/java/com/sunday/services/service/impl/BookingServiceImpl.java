package com.sunday.services.service.impl;

import java.util.UUID;
import com.sunday.common_lib.constants.BookingPermissions;
import com.sunday.common_lib.enums.AirlineStatus;
import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.common_lib.enums.PaymentGateway;
import com.sunday.common_lib.enums.TripType;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.payload.request.BookingRequest;
import com.sunday.common_lib.payload.request.PassengerRequest;
import com.sunday.common_lib.payload.request.PaymentInitiateRequest;
import com.sunday.common_lib.payload.request.SeatHoldRequest;
import com.sunday.common_lib.payload.response.*;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.clients.*;
import com.sunday.services.event.publisher.BookingEventProducer;
import com.sunday.services.integration.PricingIntegrationService;
import com.sunday.services.mapper.BookingMapper;
import com.sunday.services.mapper.PassengerMapper;
import com.sunday.services.model.Booking;
import com.sunday.services.model.BookingSaga;
import com.sunday.services.model.Passenger;
import com.sunday.services.model.SagaStatus;
import com.sunday.services.repository.BookingRepository;
import com.sunday.services.service.BookingAccessService;
import com.sunday.services.service.BookingSagaService;
import com.sunday.services.service.BookingService;
import com.sunday.services.service.PassengerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final PassengerService passengerService;
    private final BookingSagaService bookingSagaService;
    private final PricingIntegrationService pricingIntegrationService;
    private final PricingClient pricingClient;
    private final AncillaryClient ancillaryClient;
    private final PaymentClient paymentClient;
    private final SeatClient seatClient;
    private final FlightClient flightClient;
    private final BookingAccessService bookingAccess;
    private final BookingEventProducer bookingEventProducer;

    // Deliberately not @Transactional: bookingRepository.save(booking) below must commit on its own
    // (via Spring Data's default per-call transaction) before paymentClient.initiatePayment is called,
    // since payment-service calls back into this service's own /internal/bookings/{id}/payable to read
    // the booking it was just told about — a shared transaction would keep that row invisible until this
    // whole method returns, long after the callback needed it. The saga bookkeeping calls already commit
    // independently via REQUIRES_NEW (see BookingSagaServiceImpl); this mirrors that for the booking itself.
    @Override
    public PaymentInitiateResponse createBooking(BookingRequest request, UUID userId, String idempotencyKey) {
        log.info("Creating booking for user: {}", userId);

        String key = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();
        String requestHash = bookingSagaService.hashRequest(request, userId);
        BookingSaga saga = bookingSagaService.tryStart(userId, key, requestHash);
        if (!saga.getRequestHash().equals(requestHash)) {
            throw new ConflictException(String.format(ErrorMessageUtil.IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST, key));
        }
        if (saga.getStatus() == SagaStatus.COMPLETED) {
            log.info("Idempotency-Key {} already completed, returning the original response", key);
            return bookingSagaService.readResponse(saga);
        }
        if (saga.getStatus() != SagaStatus.STARTED) {
            throw new ConflictException(String.format(ErrorMessageUtil.IDEMPOTENCY_REQUEST_IN_PROGRESS, key));
        }

        Booking booking = null;
        try {
            if (request.getTripType() != TripType.ONE_WAY) {
                throw new BadRequestException(ErrorMessageUtil.ONLY_ONE_WAY_BOOKINGS_SUPPORTED);
            }

            // The flight instance is the source of truth for which flight this is — a client-supplied flightId is
            // only ever used to catch a mismatched request, never trusted on its own.
            FlightInstanceResponse flightInstanceResponse = flightClient.getFlightInstanceResponse(request.getFlightInstanceId());
            UUID flightId = flightInstanceResponse.getFlightId();
            if (request.getFlightId() != null && !request.getFlightId().equals(flightId)) {
                throw new BadRequestException(String.format(
                        ErrorMessageUtil.FLIGHT_ID_DOES_NOT_MATCH_INSTANCE, request.getFlightId(), request.getFlightInstanceId()));
            }
            LocalDate departureDate = flightInstanceResponse.getDepartureDateTime().toLocalDate();
            FlightResponse flightResponse = flightClient.getFlightById(flightId);
            requireAirlineAcceptingBookings(flightResponse);

            FareQuoteResponse quote = pricingIntegrationService.quoteFare(
                    request.getFareId(), flightId, request.getCabinClass(), departureDate);

            // Skip the call when nothing was selected: an empty list sends no body the service can read.
            List<UUID> ancillaryIds = request.getAncillaryIds();
            BigDecimal ancillaryPrice = ancillaryIds == null || ancillaryIds.isEmpty()
                    ? BigDecimal.ZERO : ancillaryClient.calculateAncillariesPrice(flightId, ancillaryIds);
            List<UUID> mealIds = request.getMealIds();
            BigDecimal mealPrice = mealIds == null || mealIds.isEmpty()
                    ? BigDecimal.ZERO : ancillaryClient.calculateMealPrice(flightId, mealIds);

            String bookingReference = generateBookingReference();
            List<UUID> seatInstanceIds = request.getPassengers().stream()
                    .map(PassengerRequest::getSeatInstanceId)
                    .collect(Collectors.toList());
            SeatHoldResponse hold = seatClient.holdSeats(SeatHoldRequest.builder()
                    .flightInstanceId(request.getFlightInstanceId())
                    .cabinClass(request.getCabinClass())
                    .seatInstanceIds(seatInstanceIds)
                    .bookingReference(bookingReference)
                    .build());
            bookingSagaService.recordSeatsHeld(saga.getId(), bookingReference, seatInstanceIds);

            Set<Passenger> passengers = new HashSet<>();
            for (PassengerRequest passengerRequest : request.getPassengers()) {
                passengers.add(passengerService.createPassengerEntity(passengerRequest, userId));
            }

            booking = BookingMapper.toEntity(request, userId, passengers, bookingReference);
            booking.setFlightId(flightId);
            booking.setStatus(BookingStatus.PENDING);
            booking.setAirlineId(flightResponse.getAirline().getId());
            booking.setSeatInstanceIds(seatInstanceIds);
            booking.setTicketTimeLimit(LocalDateTime.ofInstant(hold.getHoldExpiresAt(), ZoneId.systemDefault()));

            booking.setFareVersion(quote.getVersion());
            booking.setFareCurrency(quote.getCurrency());
            booking.setFareBaseAmount(quote.getBaseFare());
            booking.setFareTaxAmount(quote.getTaxesAndFees());
            booking.setFareFeeAmount(quote.getAirlineFees());
            booking.setFareRulesId(quote.getFareRulesId());
            booking.setFareBaggagePolicyId(quote.getBaggagePolicyId());
            booking.setFareTravelDate(departureDate);

            BigDecimal fareTotal = quote.getTotalPrice().multiply(BigDecimal.valueOf(passengers.size()));
            BigDecimal totalPrice = fareTotal.add(hold.getTotalPremium()).add(ancillaryPrice).add(mealPrice)
                    .setScale(2, RoundingMode.HALF_EVEN);
            // The stored total is what payment-service and PaymentEventListener check the payment against.
            booking.setTotalAmount(totalPrice);

            booking = bookingRepository.save(booking);
            for (Passenger passenger : passengers) {
                passenger.setBooking(booking);
            }
            bookingSagaService.recordBookingCreated(saga.getId(), booking.getId());
            // Tickets are issued once the booking is CONFIRMED (payment success), not here — see
            // PaymentEventListener.handlePaymentCompleted.

            PaymentInitiateRequest paymentRequest = PaymentInitiateRequest.builder()
                    .userId(userId)
                    .bookingId(booking.getId())
                    .amount(totalPrice)
                    .gateway(PaymentGateway.RAZORPAY)
                    .description("Booking: " + bookingReference)
                    .idempotencyKey(bookingReference)
                    .build();

            log.info("Payment initiated for booking {} amount {}", bookingReference, totalPrice);

            PaymentInitiateResponse paymentInit = paymentClient.initiatePayment(paymentRequest, userId);
            if (paymentInit == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        ErrorMessageUtil.PAYMENT_SERVICE_UNAVAILABLE);
            }
            bookingSagaService.recordPaymentInitiated(saga.getId(), paymentInit);
            return paymentInit;
        } catch (RuntimeException e) {
            log.error("createBooking failed for Idempotency-Key {}, compensating", key, e);
            if (booking != null) {
                booking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);
                bookingEventProducer.sendBookingCancelled(booking);
            }
            bookingSagaService.fail(saga.getId(), e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public BookingResponse updateBooking(UUID id, BookingRequest request, UUID userId) {
        Booking booking = findBookingOrThrow(id);
        bookingAccess.requireOwnerOrPermission(booking, userId, BookingPermissions.BOOKING_MANAGE);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ConflictException(String.format(
                    ErrorMessageUtil.BOOKING_NOT_MODIFIABLE, booking.getBookingReference(), booking.getStatus()));
        }

        Set<Passenger> passengers = new HashSet<>();
        for (PassengerRequest passengerRequest : request.getPassengers()) {
            passengers.add(passengerService.createPassengerEntity(passengerRequest, booking.getUserId()));
        }

        BookingMapper.updateEntityFromRequest(request, booking, passengers);
        Booking updated = bookingRepository.save(booking);
        return convertBookingResponse(updated, canSeePii(booking, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(UUID id, UUID userId) {
        Booking booking = bookingRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.BOOKING_NOT_FOUND_BY_ID, id)));
        bookingAccess.requireOwnerOrPermission(booking, userId, BookingPermissions.BOOKING_READ);
        return convertBookingResponse(booking, canSeePii(booking, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingForSupport(String bookingReference) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.BOOKING_NOT_FOUND_BY_REFERENCE, bookingReference)));
        return convertBookingResponse(booking, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByAirline(
            UUID userId,
            UUID airlineId,
            String searchQuery,
            BookingStatus status,
            UUID flightInstanceId,
            String sortDirection
    ) {
        bookingAccess.requireAirlinePermission(airlineId, userId, BookingPermissions.BOOKING_READ);
        boolean seePii = bookingAccess.hasAirlinePermission(airlineId, userId, BookingPermissions.PASSENGER_PII_READ);

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, "bookingDate");

        List<Booking> bookings = bookingRepository.findByAirlineWithFilters(
                airlineId, searchQuery, status, flightInstanceId, sort);

        List<BookingResponse> responses = enrichBatch(bookings);
        if (!seePii) {
            responses.forEach(this::maskPii);
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByUser(UUID userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(booking -> convertBookingResponse(booking, true))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassengerResponse> getPassengersByBooking(UUID bookingId, UUID userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.BOOKING_NOT_FOUND_BY_ID, bookingId)));
        bookingAccess.requireOwnerOrPermission(booking, userId, BookingPermissions.PASSENGER_READ);

        boolean seePii = canSeePii(booking, userId);
        return booking.getPassengers().stream()
                .map(PassengerMapper::toResponse)
                .peek(passenger -> {
                    if (!seePii) {
                        passenger.setPassportNumber(null);
                        passenger.setDateOfBirth(null);
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(UUID id, UUID userId) {
        Booking booking = findBookingOrThrow(id);
        bookingAccess.requireOwnerOrPermission(booking, userId, BookingPermissions.BOOKING_MANAGE);

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setLastModified(LocalDateTime.now());
        Booking updated = bookingRepository.save(booking);
        bookingEventProducer.sendBookingCancelled(updated, "Cancelled by request");

        log.info("Booking cancelled: {}", booking.getBookingReference());
        return convertBookingResponse(updated, canSeePii(booking, userId));
    }

    @Override
    @Transactional
    public void deleteBooking(UUID id, UUID userId) {
        Booking booking = findBookingOrThrow(id);
        bookingAccess.requireAirlinePermission(booking.getAirlineId(), userId, BookingPermissions.BOOKING_MANAGE);
        bookingEventProducer.sendBookingCancelled(booking);
        bookingRepository.delete(booking);
        log.info("Booking deleted: {}", booking.getBookingReference());
    }

    @Override
    public boolean existsById(UUID id) {
        return bookingRepository.existsById(id);
    }

    @Override
    public long count() {
        return bookingRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByFlightId(UUID userId, UUID airlineId, UUID flightInstanceId) {
        bookingAccess.requireAirlinePermission(airlineId, userId, BookingPermissions.BOOKING_READ);
        return bookingRepository.countByFlightInstanceIdAndAirlineId(flightInstanceId, airlineId);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingStatisticsResponse getBookingStatisticsForAirline(UUID userId, UUID airlineId) {
        bookingAccess.requireAirlinePermission(airlineId, userId, BookingPermissions.BOOKING_READ);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = YearMonth.now().atEndOfMonth().atTime(LocalTime.MAX);

        // Trend series (daily and monthly) are not computed yet, so they are returned empty rather than zero-filled.
        return BookingStatisticsResponse.builder()
                .totalBookingsToday(bookingRepository.countByAirlineIdAndBookingDateBetween(airlineId, startOfDay, endOfDay))
                .revenueToday(bookingRepository.sumTotalAmountByAirlineIdAndBookingDateBetween(airlineId, startOfDay, endOfDay))
                .totalBookingsThisMonth(bookingRepository.countByAirlineIdAndBookingDateBetween(airlineId, startOfMonth, endOfMonth))
                .revenueThisMonth(bookingRepository.sumTotalAmountByAirlineIdAndBookingDateBetween(airlineId, startOfMonth, endOfMonth))
                .dailyTrend(new ArrayList<>())
                .monthlyData(new ArrayList<>())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PayableBookingResponse getPayableBooking(UUID id) {
        Booking booking = findBookingOrThrow(id);
        return PayableBookingResponse.builder()
                .bookingId(booking.getId())
                .userId(booking.getUserId())
                .airlineId(booking.getAirlineId())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .build();
    }

    private Booking findBookingOrThrow(UUID id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.BOOKING_NOT_FOUND_BY_ID, id)));
    }

    /** The customer always sees their own passport and date of birth; anyone else needs PASSENGER_PII_READ on the airline. */
    private boolean canSeePii(Booking booking, UUID userId) {
        return bookingAccess.isOwner(booking, userId)
                || bookingAccess.hasAirlinePermission(booking.getAirlineId(), userId, BookingPermissions.PASSENGER_PII_READ);
    }

    private void maskPii(BookingResponse response) {
        if (response.getPassengers() != null) {
            response.getPassengers().forEach(passenger -> {
                passenger.setPassportNumber(null);
                passenger.setDateOfBirth(null);
            });
        }
    }

    /** Suspended, banned or closed airlines sell nothing new; existing bookings are untouched. */
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
        } while (bookingRepository.findByBookingReference(reference).isPresent());
        return reference;
    }

    private List<BookingResponse> enrichBatch(List<Booking> bookings) {
        if (bookings.isEmpty()) return new ArrayList<>();

        List<UUID> bookingIds = bookings.stream().map(Booking::getId).collect(Collectors.toList());

        List<UUID> fareIds = bookings.stream().map(Booking::getFareId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<UUID> flightIds = bookings.stream().map(Booking::getFlightId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<UUID> flightInstanceIds = bookings.stream().map(Booking::getFlightInstanceId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<UUID> allSeatIds = bookings.stream()
                .flatMap(b -> b.getSeatInstanceIds() != null ? b.getSeatInstanceIds().stream() : Stream.empty())
                .distinct().collect(Collectors.toList());
        List<UUID> allAncillaryIds = bookings.stream()
                .flatMap(b -> b.getAncillaryIds() != null ? b.getAncillaryIds().stream() : Stream.empty())
                .distinct().collect(Collectors.toList());
        List<UUID> allMealIds = bookings.stream()
                .flatMap(b -> b.getMealIds() != null ? b.getMealIds().stream() : Stream.empty())
                .distinct().collect(Collectors.toList());

        Map<UUID, FareResponse> fareMap = pricingClient.getFaresByIds(fareIds);
        Map<UUID, FlightResponse> flightMap = flightClient.getFlightsByIds(flightIds);
        Map<UUID, FlightInstanceResponse> flightInstanceMap = flightClient.getFlightInstancesByIds(flightInstanceIds);
        Map<UUID, PaymentDTO> paymentMap = paymentClient.getPaymentsByBookingIds(bookingIds);

        Map<UUID, SeatInstanceResponse> seatMap = allSeatIds.isEmpty()
                ? Collections.emptyMap()
                : seatClient.getAllByIds(allSeatIds).stream()
                        .collect(Collectors.toMap(SeatInstanceResponse::getId, s -> s));
        Map<UUID, FlightCabinAncillaryResponse> ancillaryMap = allAncillaryIds.isEmpty()
                ? Collections.emptyMap()
                : ancillaryClient.getAllByIds(allAncillaryIds).stream()
                        .collect(Collectors.toMap(FlightCabinAncillaryResponse::getId, a -> a));
        Map<UUID, FlightMealResponse> mealMap = allMealIds.isEmpty()
                ? Collections.emptyMap()
                : ancillaryClient.getMealsByIds(allMealIds).stream()
                        .collect(Collectors.toMap(FlightMealResponse::getId, m -> m));

        return bookings.stream().map(booking -> {
            List<SeatInstanceResponse> seats = booking.getSeatInstanceIds() != null
                    ? booking.getSeatInstanceIds().stream()
                            .map(seatMap::get).filter(Objects::nonNull).collect(Collectors.toList())
                    : Collections.emptyList();
            List<FlightCabinAncillaryResponse> ancillaries = booking.getAncillaryIds() != null
                    ? booking.getAncillaryIds().stream()
                            .map(ancillaryMap::get).filter(Objects::nonNull).collect(Collectors.toList())
                    : Collections.emptyList();
            List<FlightMealResponse> meals = booking.getMealIds() != null
                    ? booking.getMealIds().stream()
                            .map(mealMap::get).filter(Objects::nonNull).collect(Collectors.toList())
                    : Collections.emptyList();

            return BookingMapper.toResponse(
                    booking,
                    paymentMap.get(booking.getId()),
                    fareMap.get(booking.getFareId()),
                    flightMap.get(booking.getFlightId()),
                    flightInstanceMap.get(booking.getFlightInstanceId()),
                    ancillaries,
                    meals,
                    seats
            );
        }).collect(Collectors.toList());
    }

    private BookingResponse convertBookingResponse(Booking booking, boolean seePii) {
        // Bulk lookups return one response per unique id, so re-map over the booking's own (possibly repeated)
        // id list: a quantity-2 item still renders as two entries.
        List<UUID> ancillaryIds = booking.getAncillaryIds() != null ? booking.getAncillaryIds() : List.of();
        List<UUID> mealIds = booking.getMealIds() != null ? booking.getMealIds() : List.of();
        List<UUID> seatIds = booking.getSeatInstanceIds() != null ? booking.getSeatInstanceIds() : List.of();

        Map<UUID, FlightCabinAncillaryResponse> ancillaryMap = ancillaryIds.isEmpty()
                ? Collections.emptyMap()
                : ancillaryClient.getAllByIds(ancillaryIds).stream()
                        .collect(Collectors.toMap(FlightCabinAncillaryResponse::getId, a -> a));
        Map<UUID, FlightMealResponse> mealMap = mealIds.isEmpty()
                ? Collections.emptyMap()
                : ancillaryClient.getMealsByIds(mealIds).stream()
                        .collect(Collectors.toMap(FlightMealResponse::getId, m -> m));
        Map<UUID, SeatInstanceResponse> seatMap = seatIds.isEmpty()
                ? Collections.emptyMap()
                : seatClient.getAllByIds(seatIds).stream()
                        .collect(Collectors.toMap(SeatInstanceResponse::getId, s -> s));

        List<FlightCabinAncillaryResponse> ancillaryResponses = ancillaryIds.stream()
                .map(ancillaryMap::get).filter(Objects::nonNull).collect(Collectors.toList());
        List<FlightMealResponse> mealResponses = mealIds.stream()
                .map(mealMap::get).filter(Objects::nonNull).collect(Collectors.toList());
        List<SeatInstanceResponse> seatResponses = seatIds.stream()
                .map(seatMap::get).filter(Objects::nonNull).collect(Collectors.toList());

        PaymentDTO paymentDTO = paymentClient.getPaymentByBookingId(booking.getId());
        FareResponse fareResponse = pricingClient.getFareById(booking.getFareId());
        FlightResponse flightResponse = flightClient.getFlightById(booking.getFlightId());
        FlightInstanceResponse flightInstanceResponse = flightClient.getFlightInstanceResponse(booking.getFlightInstanceId());

        BookingResponse response = BookingMapper.toResponse(booking,
                paymentDTO,
                fareResponse,
                flightResponse,
                flightInstanceResponse,
                ancillaryResponses,
                mealResponses,
                seatResponses
        );
        if (!seePii) {
            maskPii(response);
        }
        return response;
    }
}
