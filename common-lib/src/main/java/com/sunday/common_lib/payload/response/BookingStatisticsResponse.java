package com.sunday.common_lib.payload.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingStatisticsResponse {

    // Daily statistics
    private Long totalBookingsToday;
    private BigDecimal revenueToday;

    // Monthly statistics
    private Long totalBookingsThisMonth;
    private BigDecimal revenueThisMonth;

    // Daily trend data (for the last 30 days)
    private List<DailyBookingData> dailyTrend;

    // Monthly revenue and bookings chart
    private List<MonthlyData> monthlyData;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyBookingData {
        private String date; // Format: yyyy-MM-dd
        private Long bookingCount;
        private BigDecimal revenue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyData {
        private String month; // Format: yyyy-MM
        private Long bookingCount;
        private BigDecimal revenue;
    }
}
