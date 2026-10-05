package com.sunday.services.event;

import com.sunday.common_lib.event.FlightInstanceAircraftChangedEvent;
import com.sunday.common_lib.event.FlightInstanceCreatedEvent;
import com.sunday.common_lib.event.FlightInstancesBulkCreatedEvent;
import com.sunday.services.service.CabinInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FlightInstanceEventConsumer {

    private final CabinInventoryService cabinInventoryService;

    @KafkaListener(topics = "flight-instance-created", groupId = "seat-service-group")
    public void handleFlightInstanceCreated(FlightInstanceCreatedEvent event) {
        cabinInventoryService.materializeForFlightInstance(event);
    }

    @KafkaListener(topics = "flight-instances-bulk-created", groupId = "seat-service-group")
    public void handleFlightInstancesBulkCreated(FlightInstancesBulkCreatedEvent event) {
        cabinInventoryService.materializeForFlightInstances(event);
    }

    @KafkaListener(topics = "flight-instance-aircraft-changed", groupId = "seat-service-group")
    public void handleFlightInstanceAircraftChanged(FlightInstanceAircraftChangedEvent event) {
        cabinInventoryService.reconcileForAircraftChange(event);
    }
}
