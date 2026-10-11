package com.sunday.services.Integration;

import java.util.UUID;
import com.sunday.common_lib.payload.response.AircraftResponse;

import java.util.Collection;
import java.util.Map;

public interface AirlineIntegrationService {

    /**
     * Throws OperationNotPermittedException unless the user's active membership in the
     * airline grants the named permission.
     */
    void requirePermission(UUID userId, UUID airlineId, String permission);

    /**
     * Batch variant: one network call instead of one per airline (used by bulk-create paths
     * that touch several airlines). Throws if the caller lacks the permission on any of them.
     */
    void requirePermission(UUID userId, Collection<UUID> airlineIds, String permission);

    /** Permission-gated lookup — forwards the caller's identity, for mutation/ownership-sensitive paths. */
    AircraftResponse getAircraftById(UUID aircraftId, UUID userId);

    /** Unauthenticated lookup — for display-enrichment callers with no real caller identity (e.g. public search). */
    AircraftResponse getAircraftByIdInternal(UUID aircraftId);

    /** Batch variant of {@link #getAircraftByIdInternal}: one round trip instead of one per aircraft. */
    Map<UUID, AircraftResponse> getAircraftsByIdsInternal(Collection<UUID> aircraftIds);
}
