package com.sunday.common_lib.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Departures flight-ops removed (none had a seat sold or held); seat-service drops their seat inventory. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightInstancesDeletedEvent {

    public static final String TOPIC = "flight-instances-deleted";

    private List<UUID> flightInstanceIds;
}
