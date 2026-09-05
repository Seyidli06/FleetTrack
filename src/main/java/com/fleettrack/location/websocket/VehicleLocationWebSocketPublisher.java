package com.fleettrack.location.websocket;

import com.fleettrack.location.dto.VehicleLocationResponse;
import com.fleettrack.location.event.VehicleLocationCreatedEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class VehicleLocationWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public VehicleLocationWebSocketPublisher(
            SimpMessagingTemplate messagingTemplate
    ) {
        this.messagingTemplate =
                messagingTemplate;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void publish(
            VehicleLocationCreatedEvent event
    ) {
        VehicleLocationResponse location =
                event.location();

        String destination =
                "/topic/vehicles/"
                        + location.vehicleId()
                        + "/location";

        messagingTemplate.convertAndSend(
                destination,
                location
        );
    }
}