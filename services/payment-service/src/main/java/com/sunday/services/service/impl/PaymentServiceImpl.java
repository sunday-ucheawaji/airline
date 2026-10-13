package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.PaymentPermissions;
import com.sunday.common_lib.dto.UserDTO;
import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.common_lib.enums.PaymentGateway;
import com.sunday.common_lib.enums.PaymentStatus;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.OperationNotPermittedException;
import com.sunday.common_lib.exception.PaymentException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.payload.request.PaymentInitiateRequest;
import com.sunday.common_lib.payload.response.PaymentDTO;
import com.sunday.common_lib.payload.response.PaymentInitiateResponse;
import com.sunday.common_lib.payload.response.PayableBookingResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.client.BookingClient;
import com.sunday.services.client.UserClient;
import com.sunday.services.event.PaymentEventProducer;
import com.sunday.services.mapper.PaymentMapper;
import com.sunday.services.model.Payment;
import com.sunday.services.model.ProcessedWebhookEvent;
import com.sunday.services.repository.PaymentRepository;
import com.sunday.services.repository.ProcessedWebhookEventRepository;
import com.sunday.services.service.PaymentAccessService;
import com.sunday.services.service.PaymentService;
import com.sunday.services.service.gateway.MoneyUtil;
import com.sunday.services.service.gateway.PaymentGatewayProvider;
import com.sunday.services.service.gateway.PaymentGatewayRegistry;
import com.sunday.services.service.gateway.ProviderCheckoutResult;
import com.sunday.services.service.gateway.ProviderPaymentStatus;
import com.sunday.services.service.gateway.ProviderRefundResult;
import com.sunday.services.service.gateway.ProviderWebhookEvent;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final ProcessedWebhookEventRepository processedWebhookEventRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final PaymentGatewayRegistry gatewayRegistry;
    private final PaymentAccessService paymentAccessService;
    private final UserClient userClient;
    private final BookingClient bookingClient;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    @Transactional
    public PaymentInitiateResponse initiatePayment(PaymentInitiateRequest request) {
        if (request.getIdempotencyKey() != null) {
            Optional<PaymentInitiateResponse> replay = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey())
                    .map(this::readStoredInitiateResponse);
            if (replay.isPresent()) {
                log.info("Idempotency-Key {} already initiated a payment, returning the original response", request.getIdempotencyKey());
                return replay.get();
            }
        }

        PayableBookingResponse booking = fetchPayableBooking(request.getBookingId());
        if (!request.getUserId().equals(booking.getUserId())) {
            throw new OperationNotPermittedException(ErrorMessageUtil.BOOKING_PAYMENT_NOT_OWNED);
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ConflictException(String.format(
                    ErrorMessageUtil.BOOKING_NOT_PAYABLE, booking.getBookingId(), booking.getStatus()));
        }
        if (booking.getTotalAmount() == null || amountsDiffer(request.getAmount(), booking.getTotalAmount())) {
            throw new ConflictException(String.format(
                    ErrorMessageUtil.BOOKING_AMOUNT_MISMATCH, request.getAmount(), booking.getTotalAmount()));
        }
        if (!request.getCurrency().equals(booking.getCurrency())) {
            throw new ConflictException(String.format(
                    ErrorMessageUtil.PAYMENT_CURRENCY_MISMATCH, request.getCurrency(), booking.getCurrency()));
        }
        MoneyUtil.requireSupported(request.getCurrency());
        paymentRepository.findFirstByBookingIdOrderByIdDesc(request.getBookingId())
                .filter(existing -> existing.getStatus() == PaymentStatus.SUCCESS)
                .ifPresent(existing -> {
                    throw new ConflictException(ErrorMessageUtil.PAYMENT_ALREADY_COMPLETED);
                });

        try {
            log.info("Initiating payment for user: {} with gateway: {}",
                    request.getUserId(), request.getGateway());

            Payment payment = Payment.builder()
                    .userId(request.getUserId())
                    .bookingId(request.getBookingId())
                    .airlineId(booking.getAirlineId())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .provider(request.getGateway())
                    .status(PaymentStatus.PENDING)
                    .transactionId(generateTransactionId())
                    .idempotencyKey(request.getIdempotencyKey())
                    .build();

            try {
                payment = paymentRepository.saveAndFlush(payment);
            } catch (DataIntegrityViolationException e) {
                // Lost a race with a concurrent call carrying the same Idempotency-Key — the winner's
                // row is the authoritative one; return its already-computed response instead of erroring.
                return paymentRepository.findByIdempotencyKey(request.getIdempotencyKey())
                        .map(this::readStoredInitiateResponse)
                        .orElseThrow(() -> e);
            }

            PaymentGatewayProvider provider = gatewayRegistry.resolve(request.getGateway());
            UserDTO payer = userClient.getUserById(payment.getUserId());
            ProviderCheckoutResult checkout = provider.createCheckout(payment, payer);
            payment.setProviderPaymentId(checkout.providerReference());

            PaymentInitiateResponse response = PaymentInitiateResponse.builder()
                    .paymentId(payment.getId())
                    .gateway(request.getGateway())
                    .transactionId(payment.getTransactionId())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .description(request.getDescription())
                    .success(true)
                    .message("Payment initiated successfully")
                    .checkoutUrl(checkout.checkoutUrl())
                    // Historically Razorpay-only; now holds whichever provider's own checkout reference.
                    .razorpayOrderId(checkout.providerReference())
                    .build();

            if (payment.getIdempotencyKey() != null) {
                payment.setInitiateResponseJson(writeJson(response));
            }
            paymentRepository.save(payment);

            log.info("Payment initiated successfully with ID: {}", payment.getId());
            return response;

        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error initiating payment: {}", e.getMessage(), e);
            throw new PaymentException(String.format(ErrorMessageUtil.PAYMENT_INITIATION_FAILED, e.getMessage()));
        }
    }

    @Override
    @Transactional
    public PaymentDTO handleWebhook(PaymentGateway gateway, byte[] rawBody, String signatureHeaderValue) {
        PaymentGatewayProvider provider = gatewayRegistry.resolve(gateway);
        if (!provider.verifySignature(rawBody, signatureHeaderValue)) {
            log.warn("Rejected {} webhook: signature verification failed", gateway);
            throw new BadRequestException(ErrorMessageUtil.WEBHOOK_SIGNATURE_INVALID);
        }

        ProviderWebhookEvent event = provider.parseWebhookEvent(rawBody);
        if (event.internalPaymentId() == null) {
            throw new PaymentException(String.format(ErrorMessageUtil.PAYMENT_NOT_FOUND_BY_ID, "unknown"));
        }

        // Deliberately not REQUIRES_NEW: the claim must commit atomically with the payment-status
        // update that follows it, in the same transaction, so a redelivery after a genuine failure can
        // still retry — see booking-service's BookingSagaServiceImpl.claimPaymentEvent for the rationale.
        if (!claimWebhookEvent(gateway.name(), event.internalPaymentId() + ":" + event.eventType())) {
            log.info("Duplicate {} webhook for payment {} event {}, skipping reprocessing",
                    gateway, event.internalPaymentId(), event.eventType());
            return paymentRepository.findById(event.internalPaymentId()).map(PaymentMapper::toDTO).orElse(null);
        }

        Payment payment = paymentRepository.findById(event.internalPaymentId())
                .orElseThrow(() -> new PaymentException(String.format(ErrorMessageUtil.PAYMENT_NOT_FOUND_BY_ID, event.internalPaymentId())));

        // Authoritative server-to-server cross-check — never trust the webhook body's status/amount alone.
        ProviderPaymentStatus authoritative = provider.fetchStatus(event.providerReference());

        String failureReason = null;
        if (authoritative.status() != PaymentStatus.SUCCESS) {
            failureReason = ErrorMessageUtil.PAYMENT_VERIFICATION_FAILED;
        } else if (authoritative.currency() == null || !authoritative.currency().equalsIgnoreCase(payment.getCurrency())
                || amountsDiffer(authoritative.capturedAmount(), payment.getAmount())) {
            failureReason = ErrorMessageUtil.PAYMENT_CAPTURED_AMOUNT_MISMATCH;
            log.error("Payment {} captured {} via {}, expected {}", payment.getId(), authoritative.capturedAmount(), gateway, payment.getAmount());
        }
        boolean isValid = failureReason == null;

        if (isValid) {
            payment.setProviderPaymentId(event.providerReference());
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);
            paymentEventProducer.sendPaymentCompleted(payment);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(failureReason);
            log.error("Payment verification failed for payment {}: {}", payment.getId(), failureReason);
            payment = paymentRepository.save(payment);
            paymentEventProducer.sendPaymentFailed(payment, userClient.getUserById(payment.getUserId()));
        }

        return PaymentMapper.toDTO(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDTO getPaymentForBooking(UUID bookingId, UUID userId) {
        Payment payment = paymentRepository.findFirstByBookingIdOrderByIdDesc(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.PAYMENT_NOT_FOUND_FOR_BOOKING, bookingId)));
        paymentAccessService.requireOwnerOrPermission(payment, userId, PaymentPermissions.PAYMENT_READ);
        return PaymentMapper.toDTO(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentDTO> getPaymentsForAirline(UUID airlineId, UUID userId, Pageable pageable) {
        paymentAccessService.requireAirlinePermission(airlineId, userId, PaymentPermissions.PAYMENT_READ);
        return paymentRepository.findByAirlineId(airlineId, pageable).map(PaymentMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDTO getPaymentForSupport(UUID bookingId) {
        // Permission (PAYMENT_SUPPORT_READ) is checked at the gateway, same as booking-service's
        // getBookingForSupport — no sensitive field exists on Payment today, so nothing to mask.
        return paymentRepository.findFirstByBookingIdOrderByIdDesc(bookingId)
                .map(PaymentMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.PAYMENT_NOT_FOUND_FOR_BOOKING, bookingId)));
    }

    @Override
    @Transactional
    public PaymentDTO refundPayment(UUID paymentId, UUID userId, BigDecimal amount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.PAYMENT_NOT_FOUND_BY_ID, paymentId)));

        // Never owner-or-permission: a customer must never execute their own refund.
        paymentAccessService.requireAirlinePermission(payment.getAirlineId(), userId, PaymentPermissions.PAYMENT_REFUND_EXECUTE);

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new ConflictException(String.format(ErrorMessageUtil.PAYMENT_NOT_REFUNDABLE, paymentId, payment.getStatus()));
        }
        return executeRefund(payment, refundAmount(payment, amount));
    }

    @Override
    @Transactional
    public PaymentDTO refundPaymentAsSystem(UUID paymentId, BigDecimal amount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.PAYMENT_NOT_FOUND_BY_ID, paymentId)));
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return PaymentMapper.toDTO(payment);
        }
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new ConflictException(String.format(ErrorMessageUtil.PAYMENT_NOT_REFUNDABLE, paymentId, payment.getStatus()));
        }
        return executeRefund(payment, refundAmount(payment, amount));
    }

    private static BigDecimal refundAmount(Payment payment, BigDecimal requested) {
        if (requested == null) {
            return payment.getAmount();
        }
        if (requested.signum() <= 0 || requested.compareTo(payment.getAmount()) > 0) {
            throw new BadRequestException(String.format(ErrorMessageUtil.PAYMENT_REFUND_AMOUNT_INVALID, requested, payment.getAmount()));
        }
        return requested;
    }

    private PaymentDTO executeRefund(Payment payment, BigDecimal refundAmount) {
        UUID paymentId = payment.getId();
        PaymentGatewayProvider provider = gatewayRegistry.resolve(payment.getProvider());
        try {
            ProviderRefundResult result = provider.initiateRefund(payment, refundAmount);
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundId(result.providerRefundId());
            payment.setRefundedAmount(refundAmount);
            payment = paymentRepository.save(payment);
            paymentEventProducer.sendPaymentRefunded(payment, userClient.getUserById(payment.getUserId()));
            log.info("Payment {} refunded via {} (provider refund id {})", paymentId, payment.getProvider(), result.providerRefundId());
            return PaymentMapper.toDTO(payment);
        } catch (PaymentException e) {
            // Do not flip status on a provider-side failure — the money never moved; a retry is safe.
            throw new PaymentException(String.format(ErrorMessageUtil.PAYMENT_REFUND_FAILED, paymentId, e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDTO getPaymentByBookingId(UUID bookingId) {
        return paymentRepository.findFirstByBookingIdOrderByIdDesc(bookingId)
                .map(PaymentMapper::toDTO)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, PaymentDTO> getPaymentsByBookingIds(List<UUID> bookingIds) {
        if (bookingIds == null || bookingIds.isEmpty()) return Map.of();
        return paymentRepository.findByBookingIdIn(bookingIds).stream()
                .collect(Collectors.toMap(Payment::getBookingId, PaymentMapper::toDTO,
                        (earlier, later) -> earlier.getId().compareTo(later.getId()) > 0 ? earlier : later));
    }

    private boolean amountsDiffer(BigDecimal a, BigDecimal b) {
        return a.setScale(2, RoundingMode.HALF_EVEN).compareTo(b.setScale(2, RoundingMode.HALF_EVEN)) != 0;
    }

    private PayableBookingResponse fetchPayableBooking(UUID bookingId) {
        try {
            return bookingClient.getPayableBooking(bookingId);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException(String.format(ErrorMessageUtil.BOOKING_NOT_FOUND_BY_ID, bookingId));
        } catch (FeignException e) {
            throw new ServiceUnavailableException(ErrorMessageUtil.BOOKING_SERVICE_UNAVAILABLE, e);
        }
    }

    private String generateTransactionId() {
        return "TXN_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Deliberately not REQUIRES_NEW: the claim must commit atomically with the business update that
     * follows it, in the same transaction — see BookingSagaServiceImpl.claimPaymentEvent (booking-service)
     * for the full rationale behind this exact shape.
     */
    private boolean claimWebhookEvent(String provider, String eventReference) {
        try {
            processedWebhookEventRepository.saveAndFlush(
                    ProcessedWebhookEvent.builder().provider(provider).eventReference(eventReference).build());
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    private PaymentInitiateResponse readStoredInitiateResponse(Payment payment) {
        if (payment.getInitiateResponseJson() == null) {
            // A concurrent call with the same key is still mid-flight (hasn't reached the gateway
            // checkout step yet) — a narrow, accepted race; the caller should retry shortly.
            throw new ConflictException(String.format(ErrorMessageUtil.IDEMPOTENCY_REQUEST_IN_PROGRESS, payment.getIdempotencyKey()));
        }
        try {
            return objectMapper.readValue(payment.getInitiateResponseJson(), PaymentInitiateResponse.class);
        } catch (Exception e) {
            throw new PaymentException("Unable to read stored initiate response for payment " + payment.getId());
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new PaymentException("Unable to snapshot initiate response: " + e.getMessage());
        }
    }
}
