package com.sunday.services.event;

import com.sunday.common_lib.event.FlightInstanceAircraftChangedEvent;
import com.sunday.common_lib.event.FlightInstanceCreatedEvent;
import com.sunday.common_lib.event.FlightInstancesBulkCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FlightInstanceEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendFlightInstanceCreated(FlightInstanceCreatedEvent event) {
        kafkaTemplate.send("flight-instance-created", event);
    }

    /** One event for the whole batch instead of one FlightInstanceCreatedEvent per instance — see FlightInstancesBulkCreatedEvent. */
    public void sendFlightInstancesBulkCreated(FlightInstancesBulkCreatedEvent event) {
        kafkaTemplate.send("flight-instances-bulk-created", event);
    }

    public void sendFlightInstanceAircraftChanged(FlightInstanceAircraftChangedEvent event) {
        kafkaTemplate.send("flight-instance-aircraft-changed", event);
    }
}
