package com.sunday.services.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunday.common_lib.payload.request.BookingRequest;
import com.sunday.common_lib.payload.response.PaymentInitiateResponse;
import com.sunday.services.model.BookingSaga;
import com.sunday.services.model.ProcessedPaymentEvent;
import com.sunday.services.model.SagaStatus;
import com.sunday.services.repository.BookingSagaRepository;
import com.sunday.services.repository.ProcessedPaymentEventRepository;
import com.sunday.services.service.BookingSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingSagaServiceImpl implements BookingSagaService {

    private final BookingSagaRepository bookingSagaRepository;
    private final ProcessedPaymentEventRepository processedPaymentEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String hashRequest(BookingRequest request, Long userId) {
        try {
            String canonical = userId + ":" + objectMapper.writeValueAsString(request);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Unable to hash booking request for idempotency", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BookingSaga tryStart(Long userId, String idempotencyKey, String requestHash) {
        Optional<BookingSaga> existing = bookingSagaRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existing.isPresent()) {
            BookingSaga saga = existing.get();
            boolean retryable = saga.getStatus() == SagaStatus.FAILED || saga.getStatus() == SagaStatus.CANCELLED;
            if (retryable && saga.getRequestHash().equals(requestHash)) {
                saga.setStatus(SagaStatus.STARTED);
                saga.setBookingId(null);
                saga.setBookingReference(null);
                saga.setResponseJson(null);
                saga.setLastError(null);
                saga.setSeatInstanceIds(null);
                return bookingSagaRepository.save(saga);
            }
            return saga;
        }
        try {
            BookingSaga saga = BookingSaga.builder()
                    .idempotencyKey(idempotencyKey)
                    .requestHash(requestHash)
                    .userId(userId)
                    .status(SagaStatus.STARTED)
                    .build();
            return bookingSagaRepository.saveAndFlush(saga);
        } catch (DataIntegrityViolationException e) {
            return bookingSagaRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey).orElseThrow(() -> e);
        }
    }

    @Override
    public PaymentInitiateResponse readResponse(BookingSaga saga) {
        try {
            return objectMapper.readValue(saga.getResponseJson(), PaymentInitiateResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read stored response for saga " + saga.getId(), e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSeatsHeld(Long sagaId, String bookingReference, List<Long> seatInstanceIds) {
        BookingSaga saga = bookingSagaRepository.findById(sagaId).orElseThrow();
        saga.setBookingReference(bookingReference);
        saga.setSeatInstanceIds(seatInstanceIds);
        saga.setStatus(SagaStatus.SEATS_HELD);
        bookingSagaRepository.save(saga);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordBookingCreated(Long sagaId, Long bookingId) {
        BookingSaga saga = bookingSagaRepository.findById(sagaId).orElseThrow();
        saga.setBookingId(bookingId);
        saga.setStatus(SagaStatus.BOOKING_CREATED);
        bookingSagaRepository.save(saga);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPaymentInitiated(Long sagaId, PaymentInitiateResponse response) {
        BookingSaga saga = bookingSagaRepository.findById(sagaId).orElseThrow();
        try {
            saga.setResponseJson(objectMapper.writeValueAsString(response));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Unable to snapshot payment response for saga " + sagaId, e);
        }
        saga.setStatus(SagaStatus.PAYMENT_INITIATED);
        bookingSagaRepository.save(saga);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(Long bookingId) {
        bookingSagaRepository.findByBookingId(bookingId).ifPresent(saga -> {
            saga.setStatus(SagaStatus.COMPLETED);
            bookingSagaRepository.save(saga);
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancel(Long bookingId) {
        bookingSagaRepository.findByBookingId(bookingId).ifPresent(saga -> {
            saga.setStatus(SagaStatus.CANCELLED);
            bookingSagaRepository.save(saga);
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long sagaId, String error) {
        bookingSagaRepository.findById(sagaId).ifPresent(saga -> {
            saga.setStatus(saga.getBookingId() != null ? SagaStatus.CANCELLED : SagaStatus.FAILED);
            saga.setLastError(error);
            bookingSagaRepository.save(saga);
        });
    }

    /**
     * Deliberately NOT REQUIRES_NEW: the claim must commit atomically with the listener's own business
     * update (same transaction), not independently of it. If it were pre-committed and the rest of the
     * listener's processing then failed and rolled back, a legitimate Kafka redelivery would find the
     * event already "claimed" and silently skip reprocessing it — the business effect would never
     * happen. Called as the first statement of the listener's own @Transactional method so a caught
     * constraint violation here never risks touching an already-poisoned session afterward.
     */
    @Override
    public boolean claimPaymentEvent(Long paymentId, String eventType) {
        try {
            processedPaymentEventRepository.saveAndFlush(
                    ProcessedPaymentEvent.builder().paymentId(paymentId).eventType(eventType).build());
            return true;
        } catch (DataIntegrityViolationException e) {
            log.info("Payment event {} for payment {} already processed, skipping redelivery", eventType, paymentId);
            return false;
        }
    }
}
