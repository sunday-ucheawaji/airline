package com.sunday.common_lib.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Published once per bulk instance creation (e.g. schedule generation) instead of one FlightInstanceCreatedEvent per instance — all instances here share the same flight/aircraft, so seat-service can resolve the aircraft's cabin configuration once and materialize every instance's seats in one batch. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightInstancesBulkCreatedEvent {
    private UUID flightId;
    private UUID aircraftId;
    private List<UUID> flightInstanceIds;
}
