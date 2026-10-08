package com.sunday.services.service.gateway;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Razorpay (paise), Stripe (cents) and Paystack (kobo) all use the smallest-currency-unit convention
 *  for a 2-decimal currency — one shared conversion instead of repeating it per adapter. */
public final class MoneyUtil {

    private MoneyUtil() {}

    public static long toSmallestUnit(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    public static BigDecimal fromSmallestUnit(long smallestUnit) {
        return BigDecimal.valueOf(smallestUnit).movePointLeft(2);
    }
}
