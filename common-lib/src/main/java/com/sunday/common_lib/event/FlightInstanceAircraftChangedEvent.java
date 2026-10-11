package com.sunday.common_lib.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/** Published after flight-ops-service commits an aircraft reassignment on a FlightInstance, so seat-service can reconcile its cabin/seat inventory. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightInstanceAircraftChangedEvent {
    private UUID flightInstanceId;
    private UUID flightId;
    private UUID oldAircraftId;
    private UUID newAircraftId;
}
