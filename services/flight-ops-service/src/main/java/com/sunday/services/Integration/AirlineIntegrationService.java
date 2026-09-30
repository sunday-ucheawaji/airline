package com.sunday.services.Integration;

import com.sunday.common_lib.payload.response.AircraftResponse;

import java.util.Collection;
import java.util.Map;

public interface AirlineIntegrationService {

    /**
     * Throws OperationNotPermittedException unless the user's active membership in the
     * airline grants the named permission.
     */
    void requirePermission(Long userId, Long airlineId, String permission);

    /**
     * Batch variant: one network call instead of one per airline (used by bulk-create paths
     * that touch several airlines). Throws if the caller lacks the permission on any of them.
     */
    void requirePermission(Long userId, Collection<Long> airlineIds, String permission);

    /** Permission-gated lookup — forwards the caller's identity, for mutation/ownership-sensitive paths. */
    AircraftResponse getAircraftById(Long aircraftId, Long userId);

    /** Unauthenticated lookup — for display-enrichment callers with no real caller identity (e.g. public search). */
    AircraftResponse getAircraftByIdInternal(Long aircraftId);

    /** Batch variant of {@link #getAircraftByIdInternal}: one round trip instead of one per aircraft. */
    Map<Long, AircraftResponse> getAircraftsByIdsInternal(Collection<Long> aircraftIds);
}
