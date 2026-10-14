package com.sunday.services.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyTotal {

    LocalDate getBookingDay();

    String getCurrency();

    Long getBookings();

    BigDecimal getTotal();
}
