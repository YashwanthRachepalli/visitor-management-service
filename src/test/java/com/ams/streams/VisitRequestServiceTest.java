package com.ams.streams;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.ams.streams.model.RequestStatus;
import com.ams.streams.model.VisitRequest;
import com.ams.streams.model.VisitRequestStatus;
import com.ams.streams.properties.KafkaStreamsProperties;
import com.ams.streams.service.VisitRequestService;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class VisitRequestServiceTest {

    @Mock
    private KafkaTemplate kafkaTemplate;

    @Mock
    private KafkaStreamsProperties kafkaStreamsProperties;

    @InjectMocks
    private VisitRequestService visitRequestService;

    @Test
    public void testCreateVisitRequest() {
        String uuid = UUID.randomUUID().toString();
        VisitRequest request = VisitRequest.builder()
                .apartmentId("apartmentId").visitorId("visitorId")
                .status(Optional.of(RequestStatus.COMPLETED))
                .requestId(Optional.of(uuid))
                .build();
        when(kafkaStreamsProperties.getVisitEventsTopic()).thenReturn("input-topic");
        when(kafkaTemplate.send(any(),any(),any())).thenReturn(new SettableListenableFuture());

        VisitRequestStatus status = visitRequestService.createVisitRequest(request);
        MatcherAssert.assertThat(status.getRequestStatus(), Matchers.equalTo(RequestStatus.COMPLETED.name()));
    }
}
