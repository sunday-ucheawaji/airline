package com.sunday.services.service.impl;

import com.sunday.common_lib.enums.SeatAvailabilityStatus;
import com.sunday.services.model.SeatInstance;
import com.sunday.services.repository.SeatInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Releases expired seat holds back to AVAILABLE. This is the actual correctness guarantee behind seat holds — it
 * bounds every stranding scenario (payment never initiated, payment-service down, browser closed) to at most one
 * hold duration, with no dependency on any Kafka event ever firing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatHoldExpirySweeper {

    private final SeatInstanceRepository seatInstanceRepository;

    @Scheduled(fixedDelayString = "${seat.hold-sweep-interval:5m}")
    @Transactional
    public void sweep() {
        List<SeatInstance> expired = seatInstanceRepository.findByStatusAndHeldUntilBefore(
                SeatAvailabilityStatus.HELD, Instant.now());
        if (expired.isEmpty()) {
            return;
        }
        for (SeatInstance si : expired) {
            si.setStatus(SeatAvailabilityStatus.AVAILABLE);
            si.setHeldUntil(null);
            si.setHeldByBookingReference(null);
        }
        seatInstanceRepository.saveAll(expired);
        log.info("Released {} expired seat hold(s)", expired.size());
    }
}
