package com.sunday.common_lib.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightInstanceCreatedEvent {
    private UUID flightInstanceId;
    private UUID aircraftId;
    private UUID flightId;
}
