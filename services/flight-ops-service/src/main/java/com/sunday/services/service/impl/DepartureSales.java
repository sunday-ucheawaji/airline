package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.client.SeatClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Seats held, booked or occupied per departure, as seat-service counts them. */
@Component
@RequiredArgsConstructor
public class DepartureSales {

    private final SeatClient seatClient;

    /** Fails closed: a change that is only safe without sales is refused when seat-service can't answer. */
    public Map<UUID, Long> soldByDeparture(Collection<UUID> departureIds) {
        if (departureIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> sold = seatClient.countSoldSeats(List.copyOf(departureIds));
        if (sold == null) {
            throw new ServiceUnavailableException(ErrorMessageUtil.SEAT_SERVICE_UNAVAILABLE);
        }
        return sold;
    }

    public long soldFor(UUID departureId) {
        return soldByDeparture(List.of(departureId)).getOrDefault(departureId, 0L);
    }

    /** For display only: empty when seat-service can't answer, so a list still loads. */
    public Map<UUID, Long> soldByDepartureForDisplay(Collection<UUID> departureIds) {
        if (departureIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> sold = seatClient.countSoldSeats(List.copyOf(departureIds));
        return sold == null ? Map.of() : sold;
    }
}
