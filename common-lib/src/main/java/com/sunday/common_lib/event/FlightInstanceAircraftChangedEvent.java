package com.sunday.common_lib.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Published after flight-ops-service commits an aircraft reassignment on a FlightInstance, so seat-service can reconcile its cabin/seat inventory. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightInstanceAircraftChangedEvent {
    private Long flightInstanceId;
    private Long flightId;
    private Long oldAircraftId;
    private Long newAircraftId;
}
