package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.BookingPermissions;
import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.common_lib.enums.PaymentGateway;
import com.sunday.common_lib.event.PaymentCompletedEvent;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.payload.request.ExtrasOrderRequest;
import com.sunday.common_lib.payload.request.PaymentInitiateRequest;
import com.sunday.common_lib.payload.response.ExtrasOrderResponse;
import com.sunday.common_lib.payload.response.PaymentInitiateResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.clients.AncillaryClient;
import com.sunday.services.clients.PaymentClient;
import com.sunday.services.config.BookingPaymentProperties;
import com.sunday.services.event.publisher.BookingEventProducer;
import com.sunday.services.mapper.ExtrasOrderMapper;
import com.sunday.services.model.Booking;
import com.sunday.services.model.ExtrasOrder;
import com.sunday.services.repository.BookingRepository;
import com.sunday.services.repository.ExtrasOrderRepository;
import com.sunday.services.service.BookingAccessService;
import com.sunday.services.service.ExtrasOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtrasOrderServiceImpl implements ExtrasOrderService {

    private static final List<BookingStatus> LIVE = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final BookingRepository bookingRepository;
    private final ExtrasOrderRepository extrasOrderRepository;
    private final BookingAccessService bookingAccess;
    private final AncillaryClient ancillaryClient;
    private final PaymentClient paymentClient;
    private final BookingPaymentProperties paymentProperties;
    private final BookingEventProducer bookingEventProducer;
    private final Clock clock;

    @Override
    public ExtrasOrderResponse orderExtras(UUID bookingId, ExtrasOrderRequest request, UUID userId, String idempotencyKey) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.BOOKING_NOT_FOUND_BY_ID, bookingId)));
        bookingAccess.requireOwnerOrPermission(booking, userId, BookingPermissions.BOOKING_MANAGE);
        if (idempotencyKey != null) {
            ExtrasOrder existing = extrasOrderRepository.findByBookingIdAndIdempotencyKey(bookingId, idempotencyKey).orElse(null);
            if (existing != null) {
                return replay(booking, existing, request, idempotencyKey);
            }
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException(String.format(
                    ErrorMessageUtil.EXTRAS_BOOKING_NOT_CONFIRMED, booking.getBookingReference(), booking.getStatus()));
        }
        if (booking.getFareTravelDate() != null && booking.getFareTravelDate().isBefore(LocalDate.now(clock))) {
            throw new ConflictException(String.format(ErrorMessageUtil.EXTRAS_AFTER_DEPARTURE, booking.getBookingReference()));
        }
        List<UUID> ancillaryIds = request.getAncillaryIds() != null ? request.getAncillaryIds() : List.of();
        List<UUID> mealIds = request.getMealIds() != null ? request.getMealIds() : List.of();
        if (ancillaryIds.isEmpty() && mealIds.isEmpty()) {
            throw new BadRequestException(ErrorMessageUtil.EXTRAS_EMPTY);
        }
        PaymentGateway gateway = paymentProperties.gatewayFor(booking.getFareCurrency());
        if (gateway == null) {
            throw new ConflictException(String.format(ErrorMessageUtil.BOOKING_CURRENCY_NOT_ACCEPTED, booking.getFareCurrency()));
        }

        BigDecimal total = price(booking, ancillaryIds, mealIds);
        ExtrasOrder order = extrasOrderRepository.save(ExtrasOrder.builder()
                .bookingId(booking.getId())
                .userId(booking.getUserId())
                .airlineId(booking.getAirlineId())
                .status(total.signum() > 0 ? BookingStatus.PENDING : BookingStatus.CONFIRMED)
                .currency(booking.getFareCurrency())
                .totalAmount(total)
                .ancillaryIds(new ArrayList<>(ancillaryIds))
                .mealIds(new ArrayList<>(mealIds))
                .idempotencyKey(idempotencyKey)
                .build());
        if (total.signum() == 0) {
            bookingEventProducer.sendExtrasConfirmed(booking, order);
            return ExtrasOrderMapper.toResponse(order, null);
        }

        try {
            return ExtrasOrderMapper.toResponse(order, startPayment(booking, order, gateway));
        } catch (RuntimeException e) {
            order.setStatus(BookingStatus.CANCELLED);
            extrasOrderRepository.save(order);
            throw e;
        }
    }

    /** A retry with the same key: the same items get the same order, and a pending one the same payment. */
    private ExtrasOrderResponse replay(Booking booking, ExtrasOrder order, ExtrasOrderRequest request, String idempotencyKey) {
        if (!sameItems(order.getAncillaryIds(), request.getAncillaryIds()) || !sameItems(order.getMealIds(), request.getMealIds())) {
            throw new ConflictException(String.format(ErrorMessageUtil.IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST, idempotencyKey));
        }
        if (order.getStatus() != BookingStatus.PENDING) {
            return ExtrasOrderMapper.toResponse(order, null);
        }
        return ExtrasOrderMapper.toResponse(order, startPayment(booking, order, paymentProperties.gatewayFor(order.getCurrency())));
    }

    private static boolean sameItems(List<UUID> stored, List<UUID> requested) {
        List<UUID> a = new ArrayList<>(stored != null ? stored : List.of());
        List<UUID> b = new ArrayList<>(requested != null ? requested : List.of());
        a.sort(null);
        b.sort(null);
        return a.equals(b);
    }

    /** Idempotent at payment-service by the order's id, so a replay returns the stored checkout. */
    private PaymentInitiateResponse startPayment(Booking booking, ExtrasOrder order, PaymentGateway gateway) {
        PaymentInitiateResponse payment = paymentClient.initiatePayment(PaymentInitiateRequest.builder()
                .userId(booking.getUserId())
                .bookingId(order.getId())
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .gateway(gateway)
                .description("Extras: " + booking.getBookingReference())
                .idempotencyKey("extras:" + order.getId())
                .build(), booking.getUserId());
        if (payment == null) {
            throw new ServiceUnavailableException(ErrorMessageUtil.PAYMENT_SERVICE_UNAVAILABLE);
        }
        return payment;
    }

    /**
     * Checks the new extras together with everything the booking already has or has ordered (cabin, per-passenger
     * limits, one meal each), then prices only the new ones.
     */
    private BigDecimal price(Booking booking, List<UUID> ancillaryIds, List<UUID> mealIds) {
        int passengers = booking.getPassengers().size();
        List<ExtrasOrder> live = extrasOrderRepository.findByBookingIdAndStatusIn(booking.getId(), LIVE);
        BigDecimal total = BigDecimal.ZERO;
        if (!ancillaryIds.isEmpty()) {
            List<UUID> all = new ArrayList<>(booking.getAncillaryIds() != null ? booking.getAncillaryIds() : List.of());
            live.forEach(order -> all.addAll(order.getAncillaryIds()));
            all.addAll(ancillaryIds);
            ancillaryClient.calculateAncillariesPrice(booking.getFlightId(), booking.getCabinClass(), passengers, all);
            total = total.add(ancillaryClient.calculateAncillariesPrice(booking.getFlightId(), booking.getCabinClass(), passengers, ancillaryIds));
        }
        if (!mealIds.isEmpty()) {
            List<UUID> all = new ArrayList<>(booking.getMealIds() != null ? booking.getMealIds() : List.of());
            live.forEach(order -> all.addAll(order.getMealIds()));
            all.addAll(mealIds);
            ancillaryClient.calculateMealPrice(booking.getFlightId(), passengers, all);
            total = total.add(ancillaryClient.calculateMealPrice(booking.getFlightId(), passengers, mealIds));
        }
        return total.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public void handlePaid(ExtrasOrder order, PaymentCompletedEvent event) {
        if (order.getStatus() == BookingStatus.CONFIRMED && event.getPaymentId().equals(order.getPaymentId())) {
            return;
        }
        Booking booking = bookingRepository.findById(order.getBookingId()).orElse(null);
        boolean paidInFull = event.getAmount() != null
                && event.getAmount().setScale(2, RoundingMode.HALF_EVEN).compareTo(order.getTotalAmount().setScale(2, RoundingMode.HALF_EVEN)) == 0
                && (event.getCurrency() == null || event.getCurrency().equals(order.getCurrency()));
        order.setPaymentId(event.getPaymentId());
        if (order.getStatus() != BookingStatus.PENDING || booking == null
                || booking.getStatus() != BookingStatus.CONFIRMED || !paidInFull) {
            paymentClient.refundPaymentAsSystem(event.getPaymentId(), null, "reject:" + event.getPaymentId());
            order.setStatus(BookingStatus.CANCELLED);
            extrasOrderRepository.save(order);
            log.warn("Payment {} for extras order {} was refunded: the order or its booking is no longer payable",
                    event.getPaymentId(), order.getId());
            return;
        }
        order.setStatus(BookingStatus.CONFIRMED);
        extrasOrderRepository.save(order);
        bookingEventProducer.sendExtrasConfirmed(booking, order);
        log.info("Extras order {} for booking {} confirmed", order.getId(), booking.getBookingReference());
    }

    @Override
    public void handleFailed(ExtrasOrder order) {
        if (order.getStatus() == BookingStatus.PENDING) {
            order.setStatus(BookingStatus.CANCELLED);
            extrasOrderRepository.save(order);
        }
    }

    @Override
    public void closeOrders(Booking booking, boolean refund) {
        for (ExtrasOrder order : extrasOrderRepository.findByBookingIdAndStatusIn(booking.getId(), LIVE)) {
            if (order.getStatus() == BookingStatus.CONFIRMED && refund && order.getPaymentId() != null) {
                paymentClient.refundPaymentAsSystem(order.getPaymentId(), null, "extras:" + order.getId());
                order.setRefundAmount(order.getTotalAmount());
            }
            order.setStatus(BookingStatus.CANCELLED);
            extrasOrderRepository.save(order);
        }
    }
}
