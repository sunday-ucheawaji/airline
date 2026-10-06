package com.sunday.services.validation;

import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.payload.request.FareRequest;
import com.sunday.common_lib.util.ErrorMessageUtil;

import java.util.Currency;

/**
 * Rules that bean validation cannot express: cross-field window order and real currency/RBD values.
 * Pure logic, no I/O, so it runs identically in single, bulk and update paths.
 */
public final class FareRequestValidator {

    private FareRequestValidator() {}

    public static void validate(FareRequest request) {
        if (request.getSalesFrom().isAfter(request.getSalesTo())) {
            throw new BadRequestException(ErrorMessageUtil.FARE_SALES_WINDOW_INVALID);
        }
        if (request.getTravelFrom().isAfter(request.getTravelTo())) {
            throw new BadRequestException(ErrorMessageUtil.FARE_TRAVEL_WINDOW_INVALID);
        }
        if (!isKnownCurrency(request.getCurrency())) {
            throw new BadRequestException(String.format(ErrorMessageUtil.FARE_CURRENCY_INVALID, request.getCurrency()));
        }
        Character rbd = request.getRbdCode();
        if (rbd < 'A' || rbd > 'Z') {
            throw new BadRequestException(ErrorMessageUtil.FARE_RBD_INVALID);
        }
    }

    private static boolean isKnownCurrency(String code) {
        try {
            Currency.getInstance(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
