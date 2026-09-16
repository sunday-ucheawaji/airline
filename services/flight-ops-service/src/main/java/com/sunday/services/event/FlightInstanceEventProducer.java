package com.sunday.services.event;

import com.sunday.common_lib.event.FlightInstanceCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FlightInstanceEventProducer {



    private final KafkaTemplate<String, FlightInstanceCreatedEvent> kafkaTemplate;

    public void sendFlightInstanceCreated(FlightInstanceCreatedEvent event) {
        kafkaTemplate.send("flight-instance-created", event);
    }
}
