package com.sunday.common_lib.payload.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingStatisticsResponse {

    // Daily statistics
    private Long totalBookingsToday;
    private BigDecimal revenueToday;
    private Map<String, BigDecimal> revenueTodayByCurrency;

    // Monthly statistics
    private Long totalBookingsThisMonth;
    private BigDecimal revenueThisMonth;
    private Map<String, BigDecimal> revenueThisMonthByCurrency;

    private String currency;

    // Sold (confirmed or completed) bookings per day for the last 30 days, oldest first, including empty days
    private List<DailyBookingData> dailyTrend;

    // The same per month for the last 12 months, oldest first
    private List<MonthlyData> monthlyData;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyBookingData {
        private String date; // Format: yyyy-MM-dd
        private Long bookingCount;
        // Null when the series spans more than one currency; see revenueByCurrency
        private BigDecimal revenue;
        private Map<String, BigDecimal> revenueByCurrency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyData {
        private String month; // Format: yyyy-MM
        private Long bookingCount;
        // Null when the series spans more than one currency; see revenueByCurrency
        private BigDecimal revenue;
        private Map<String, BigDecimal> revenueByCurrency;
    }
}
