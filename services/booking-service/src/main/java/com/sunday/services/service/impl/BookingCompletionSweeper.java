package com.sunday.services.service.impl;

import com.sunday.services.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/** Marks confirmed bookings COMPLETED once their departure date has passed. They can then no longer be changed or cancelled. */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCompletionSweeper {

    private final BookingRepository bookingRepository;
    private final Clock clock;

    @Scheduled(cron = "${booking.completion-cron:0 30 2 * * *}")
    @Transactional
    public void completeDepartedBookings() {
        int completed = bookingRepository.completeDepartedBefore(LocalDate.now(clock));
        if (completed > 0) {
            log.info("{} departed bookings marked completed", completed);
        }
    }
}
