package com.sunday.services.service.gateway;

import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.util.ErrorMessageUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public final class MoneyUtil {

    private MoneyUtil() {}

    public static long toSmallestUnit(BigDecimal amount, String currency) {
        return amount.movePointRight(fractionDigits(currency)).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    public static BigDecimal fromSmallestUnit(long smallestUnit, String currency) {
        return BigDecimal.valueOf(smallestUnit).movePointLeft(fractionDigits(currency));
    }

    public static String requireSupported(String currency) {
        fractionDigits(currency);
        return currency;
    }

    private static int fractionDigits(String currency) {
        try {
            return Math.max(Currency.getInstance(currency).getDefaultFractionDigits(), 0);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException(String.format(ErrorMessageUtil.PAYMENT_CURRENCY_NOT_SUPPORTED, currency));
        }
    }
}
