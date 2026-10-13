package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.BookingPermissions;
import com.sunday.common_lib.enums.AirlineStatus;
import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.common_lib.enums.TicketStatus;
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
import com.sunday.services.config.BookingPaymentProperties;
import com.sunday.services.event.publisher.BookingEventProducer;
import com.sunday.services.integration.PricingIntegrationService;
import com.sunday.services.mapper.BookingMapper;
import com.sunday.services.mapper.PassengerMapper;
import com.sunday.services.model.Booking;
import com.sunday.services.model.BookingSaga;
import com.sunday.services.model.Passenger;
import com.sunday.services.model.SagaStatus;
import com.sunday.services.repository.BookingRepository;
import com.sunday.services.repository.CurrencyTotal;
import com.sunday.services.service.BookingAccessService;
import com.sunday.services.service.BookingSagaService;
import com.sunday.services.service.BookingService;
import com.sunday.services.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private static final int MAX_PAGE_SIZE = 100;

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

    @Override
    public PaymentInitiateResponse createBooking(BookingRequest request, UUID userId, String idempotencyKey) {
        String key = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();
        String requestHash = bookingSagaService.hashRequest(request, userId);
        BookingSaga saga = bookingSagaService.tryStart(userId, key, requestHash);
        if (!saga.getRequestHash().equals(requestHash)) {
            throw new ConflictException(String.format(ErrorMessageUtil.IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST, key));
        }
        if (saga.getResponseJson() != null
                && (saga.getStatus() == SagaStatus.PAYMENT_INITIATED || saga.getStatus() == SagaStatus.COMPLETED)) {
            log.info("Idempotency-Key {} already initiated payment, returning the original response", key);
            return bookingSagaService.readResponse(saga);
        }
        if (saga.getStatus() != SagaStatus.STARTED) {
            throw new ConflictException(String.format(ErrorMessageUtil.IDEMPOTENCY_REQUEST_IN_PROGRESS, key));
        }

        Booking saved = null;
        String bookingReference = null;
        List<UUID> heldSeatIds = null;
        try {
            if (request.getTripType() != TripType.ONE_WAY) {
                throw new BadRequestException(ErrorMessageUtil.ONLY_ONE_WAY_BOOKINGS_SUPPORTED);
            }
            List<UUID> seatInstanceIds = requireDistinctSeats(request.getPassengers());

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

            List<UUID> ancillaryIds = request.getAncillaryIds();
            BigDecimal ancillaryPrice = ancillaryIds == null || ancillaryIds.isEmpty()
                    ? BigDecimal.ZERO : ancillaryClient.calculateAncillariesPrice(flightId, ancillaryIds);
            List<UUID> mealIds = request.getMealIds();
            BigDecimal mealPrice = mealIds == null || mealIds.isEmpty()
                    ? BigDecimal.ZERO : ancillaryClient.calculateMealPrice(flightId, mealIds);

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
                passengers.add(passenger);
            }

            Booking booking = BookingMapper.toEntity(request, userId, passengers, bookingReference);
            booking.setFlightId(flightId);
            booking.setStatus(BookingStatus.PENDING);
            booking.setAirlineId(flightResponse.getAirline().getId());
            booking.setSeatInstanceIds(new ArrayList<>(seatInstanceIds));
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

            BigDecimal fareTotal = quote.getTotalPrice().multiply(BigDecimal.valueOf(passengers.size()));
            BigDecimal totalPrice = fareTotal.add(hold.getTotalPremium()).add(ancillaryPrice).add(mealPrice)
                    .setScale(2, RoundingMode.HALF_EVEN);
            booking.setTotalAmount(totalPrice);

            for (Passenger passenger : passengers) {
                passenger.setBooking(booking);
            }
            saved = bookingRepository.save(booking);
            bookingSagaService.recordBookingCreated(saga.getId(), saved.getId());

            PaymentInitiateResponse paymentInit = paymentClient.initiatePayment(PaymentInitiateRequest.builder()
                    .userId(userId)
                    .bookingId(saved.getId())
                    .amount(totalPrice)
                    .currency(quote.getCurrency())
                    .gateway(paymentProperties.gatewayFor(quote.getCurrency()))
                    .description("Booking: " + bookingReference)
                    .idempotencyKey(bookingReference)
                    .build(), userId);
            if (paymentInit == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ErrorMessageUtil.PAYMENT_SERVICE_UNAVAILABLE);
            }
            bookingSagaService.recordPaymentInitiated(saga.getId(), paymentInit);
            log.info("Booking {} created for user {}, payment initiated for {}", bookingReference, userId, totalPrice);
            return paymentInit;
        } catch (RuntimeException e) {
            log.error("createBooking failed for Idempotency-Key {}, compensating", key, e);
            if (saved != null) {
                saved.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(saved);
            }
            if (heldSeatIds != null) {
                bookingEventProducer.sendSeatsReleased(saved != null ? saved.getId() : null, bookingReference, heldSeatIds);
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
        requireDetailsOnlyChange(booking, request);

        Map<UUID, Passenger> passengersBySeat = booking.getPassengers().stream()
                .collect(Collectors.toMap(Passenger::getSeatInstanceId, Function.identity()));
        for (PassengerRequest passengerRequest : request.getPassengers()) {
            PassengerMapper.updateEntityFromRequest(passengerRequest, passengersBySeat.get(passengerRequest.getSeatInstanceId()));
        }
        if (request.getContactInfo() != null) {
            booking.setContactInfo(request.getContactInfo());
        }
        Booking updated = bookingRepository.save(booking);
        return convertBookingResponse(updated, canSeePii(updated, userId));
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
    @Transactional(readOnly = true)
    public Page<BookingResponse> getBookingsByUser(UUID userId, int page, int size) {
        return loadPage(bookingRepository.findIdsByUserId(userId, pageRequest(page, size, Sort.Direction.DESC)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassengerResponse> getPassengersByBooking(UUID bookingId, UUID userId) {
        Booking booking = findBookingOrThrow(bookingId);
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
    @Transactional
    public BookingResponse cancelBooking(UUID id, UUID userId) {
        Booking booking = findBookingOrThrow(id);
        bookingAccess.requireOwnerOrPermission(booking, userId, BookingPermissions.BOOKING_MANAGE);
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
                paymentClient.refundPaymentAsSystem(booking.getPaymentId(), refund);
                booking.setRefundAmount(refund);
            }
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking updated = bookingRepository.save(booking);
        bookingEventProducer.sendBookingCancelled(updated, ErrorMessageUtil.BOOKING_CANCELLED_BY_REQUEST);
        bookingSagaService.cancel(updated.getId());
        log.info("Booking {} cancelled by {}", updated.getBookingReference(), userId);
        return convertBookingResponse(updated, canSeePii(updated, userId));
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

        LocalDate today = LocalDate.now(clock);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        LocalDateTime startOfMonth = YearMonth.from(today).atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = YearMonth.from(today).atEndOfMonth().atTime(LocalTime.MAX);

        Map<String, BigDecimal> revenueToday = revenueByCurrency(airlineId, startOfDay, endOfDay);
        Map<String, BigDecimal> revenueThisMonth = revenueByCurrency(airlineId, startOfMonth, endOfMonth);
        String currency = revenueThisMonth.size() == 1 ? revenueThisMonth.keySet().iterator().next() : null;
        boolean singleCurrency = revenueThisMonth.size() <= 1;

        return BookingStatisticsResponse.builder()
                .totalBookingsToday(bookingRepository.countByAirlineIdAndBookingDateBetween(airlineId, startOfDay, endOfDay))
                .revenueToday(singleCurrency ? revenueToday.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add) : null)
                .revenueTodayByCurrency(revenueToday)
                .totalBookingsThisMonth(bookingRepository.countByAirlineIdAndBookingDateBetween(airlineId, startOfMonth, endOfMonth))
                .revenueThisMonth(singleCurrency ? revenueThisMonth.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add) : null)
                .revenueThisMonthByCurrency(revenueThisMonth)
                .currency(currency)
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
        BigDecimal feePerPassenger = booking.getFareCancellationFee() != null ? booking.getFareCancellationFee() : BigDecimal.ZERO;
        BigDecimal refund = booking.getTotalAmount()
                .subtract(feePerPassenger.multiply(BigDecimal.valueOf(booking.getPassengers().size())));
        return refund.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_EVEN);
    }

    private Map<String, BigDecimal> revenueByCurrency(UUID airlineId, LocalDateTime from, LocalDateTime to) {
        return bookingRepository.sumConfirmedRevenueByCurrency(airlineId, from, to).stream()
                .collect(Collectors.toMap(CurrencyTotal::getCurrency, CurrencyTotal::getTotal, BigDecimal::add, TreeMap::new));
    }

    private static PageRequest pageRequest(int page, int size, Sort.Direction direction) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by(direction, "bookingDate"));
    }

    private Page<BookingResponse> loadPage(Page<UUID> ids) {
        Map<UUID, Booking> byId = byId(bookingRepository.findWithDetailsByIdIn(ids.getContent()), Booking::getId);
        List<Booking> bookings = ids.getContent().stream().map(byId::get).filter(Objects::nonNull).toList();
        return new PageImpl<>(enrichBatch(bookings), ids.getPageable(), ids.getTotalElements());
    }

    private static List<UUID> requireDistinctSeats(List<PassengerRequest> passengers) {
        List<UUID> seatInstanceIds = passengers.stream().map(PassengerRequest::getSeatInstanceId).toList();
        Set<UUID> seen = new HashSet<>();
        for (UUID seatInstanceId : seatInstanceIds) {
            if (!seen.add(seatInstanceId)) {
                throw new BadRequestException(String.format(ErrorMessageUtil.DUPLICATE_SEAT_IN_BOOKING, seatInstanceId));
            }
        }
        return seatInstanceIds;
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
        if (!seePii) {
            maskPii(response);
        }
        return response;
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
