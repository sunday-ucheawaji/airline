package com.sunday.services.integration;

import java.util.Collection;
import java.util.Map;

public interface PricingIntegrationService {

    /** Throws OperationNotPermittedException unless the caller's membership in the airline grants the permission. */
    void requirePermission(Long userId, Long airlineId, String permission);

    /** Batch variant: one airline-core call for all airlines. Throws if the caller lacks the permission on any. */
    void requirePermission(Long userId, Collection<Long> airlineIds, String permission);

    /**
     * One flight-ops call for all flights. Returns flightId to airlineId for each flight that exists; unknown flights
     * are absent from the map. Bulk create uses this and reports the absent ones as skipped rows.
     */
    Map<Long, Long> findAirlineIdsByFlightIds(Collection<Long> flightIds);

    /**
     * Same lookup, but throws ResourceNotFoundException if any flight is unknown, so a single fare can never be
     * written against a flight that does not exist.
     */
    Map<Long, Long> resolveAirlineIdsByFlightIds(Collection<Long> flightIds);
}
