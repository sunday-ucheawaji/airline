package com.sunday.services.validation;

import com.sunday.common_lib.enums.FareStatus;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.model.Fare;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * The single rule for whether a fare can be sold for a departure. Search, quote and publish all use it.
 * Pure logic: the caller supplies the departure date and the instant to judge at, so tests fix time.
 * Boundaries are inclusive. Advance purchase is counted in whole days between the UTC date of {@code now}
 * and the departure date.
 */
public final class FareValidityPolicy {

    private FareValidityPolicy() {}

    /** The first rule the fare breaks for this departure at this instant, or empty if it is sellable. */
    public static Optional<String> violation(Fare fare, LocalDate departureDate, Instant now) {
        if (fare.getStatus() != FareStatus.PUBLISHED) {
            return Optional.of(ErrorMessageUtil.FARE_NOT_PUBLISHED);
        }
        if (now.isBefore(fare.getSalesFrom()) || now.isAfter(fare.getSalesTo())) {
            return Optional.of(ErrorMessageUtil.FARE_OUTSIDE_SALES_WINDOW);
        }
        if (departureDate.isBefore(fare.getTravelFrom()) || departureDate.isAfter(fare.getTravelTo())) {
            return Optional.of(ErrorMessageUtil.FARE_OUTSIDE_TRAVEL_WINDOW);
        }
        if (fare.getAdvancePurchaseDays() != null) {
            LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
            if (ChronoUnit.DAYS.between(today, departureDate) < fare.getAdvancePurchaseDays()) {
                return Optional.of(String.format(ErrorMessageUtil.FARE_ADVANCE_PURCHASE_NOT_MET,
                        fare.getAdvancePurchaseDays()));
            }
        }
        return Optional.empty();
    }

    public static boolean isSellable(Fare fare, LocalDate departureDate, Instant now) {
        return violation(fare, departureDate, now).isEmpty();
    }
}
