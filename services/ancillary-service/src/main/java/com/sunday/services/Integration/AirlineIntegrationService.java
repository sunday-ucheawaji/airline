package com.sunday.services.Integration;

import com.sunday.common_lib.payload.response.AircraftResponse;

import java.util.Collection;

public interface AirlineIntegrationService {
    /** Throws OperationNotPermittedException unless the user holds an active membership in the airline. */
    void requireMembership(Long userId, Long airlineId);

    /**
     * Throws OperationNotPermittedException unless the user's active membership in the airline
     * grants the named permission. Already re-verifies membership (airline-core-service 403s if
     * none), so use this instead of requireMembership on writes, not in addition to it.
     */
    void requirePermission(Long userId, Long airlineId, String permission);

    /**
     * Batch variant: one network call instead of one per airline (used by bulk-create paths that
     * touch several airlines). Throws if the caller lacks the permission on any of them.
     */
    void requirePermission(Long userId, Collection<Long> airlineIds, String permission);

    AircraftResponse getAircraftById(Long aircraftId);
}
