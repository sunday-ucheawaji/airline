package com.sunday.services.repository;

import com.sunday.services.model.Booking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingPerformanceRepository {

    // Booking Statistics
    Long countBookingsByFlightIdAndDateRange(UUID flightId, LocalDateTime startDate, LocalDateTime endDate);

    Double sumRevenueByFlightIdAndDateRange(UUID flightId, LocalDateTime startDate, LocalDateTime endDate);

    List<Booking> findBookingsByFlightIdAndDateRange(
            UUID flightId, LocalDateTime startDate, LocalDateTime endDate);
}
