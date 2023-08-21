package com.ams.streams.service;

import com.ams.streams.event.VisitEvent;
import com.ams.streams.model.RequestStatus;
import com.ams.streams.model.VisitRequest;
import com.ams.streams.model.VisitRequestStatus;
import com.ams.streams.properties.KafkaStreamsProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class VisitRequestService {

    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Autowired
    private KafkaStreamsProperties properties;

    public VisitRequestStatus createVisitRequest(VisitRequest visitRequest) {
        VisitEvent event = createVisitEvent(visitRequest);
        kafkaTemplate.send(properties.getVisitEventsTopic(), event.getRequestId(), event);
        return VisitRequestStatus.builder()
                .requestId(event.getRequestId())
                .requestStatus(event.getRequestStatus().name())
                .build();
    }

    private VisitEvent createVisitEvent(VisitRequest visitRequest) {
        VisitEvent.VisitEventBuilder eventBuilder  = VisitEvent.builder();
        visitRequest.getRequestId().ifPresentOrElse(eventBuilder::requestId, () -> {
            UUID uuid = UUID.randomUUID();
            eventBuilder.requestId(uuid.toString());
        });
        visitRequest.getStatus().ifPresentOrElse(eventBuilder::requestStatus,
                () -> eventBuilder.requestStatus(RequestStatus.PENDING));
        return eventBuilder.apartmentId(visitRequest.getApartmentId())
                .visitorId(visitRequest.getVisitorId())
                .build();

    }
}
