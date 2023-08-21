package com.ams.streams.controller;

import static org.apache.kafka.streams.StreamsConfig.*;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ams.streams.event.VisitEvent;
import com.ams.streams.model.RequestStatus;
import com.ams.streams.model.VisitRequest;
import com.ams.streams.model.VisitRequestStatus;
import com.ams.streams.processor.VisitRequestTopology;
import com.ams.streams.properties.KafkaStreamsProperties;
import com.ams.streams.serdes.JsonNodeSerdes;
import com.ams.streams.service.VisitRequestService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class VisitRequestControllerTest {

    @Mock
    private StreamsBuilderFactoryBean factoryBean;

    @Mock
    private VisitRequestService visitRequestService;

    @InjectMocks
    private VisitRequestController visitRequestController;

    @Test
    public void testSaveVisitRequest() {
        when(visitRequestService.createVisitRequest(any()))
                .thenReturn(VisitRequestStatus.builder()
                        .requestId("testId")
                        .requestStatus(RequestStatus.DECLINED.name())
                        .build());

        ResponseEntity<VisitRequestStatus> response = visitRequestController.saveVisitRequest(VisitRequest.builder()
                .requestId(Optional.of("testId"))
                .status(Optional.of(RequestStatus.DECLINED))
                .visitorId("visitorId").apartmentId("apartmentId")
                .build());

        MatcherAssert.assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        MatcherAssert.assertThat(response.getBody().getRequestStatus(), equalTo(RequestStatus.DECLINED.name()));
    }

    @Test
    public void testGetStatus() {
        KafkaStreamsProperties properties = new KafkaStreamsProperties();
        ReflectionTestUtils.setField(properties, "visitEventsTopic", "input-topic");
        VisitRequestTopology visitRequestTopology = new VisitRequestTopology(properties);
        StreamsBuilder streamsBuilder = new StreamsBuilder();

        visitRequestTopology.buildPipeline(streamsBuilder);
        Topology topology = streamsBuilder.build();

        Properties streamsConfiguration = new Properties();
        streamsConfiguration.put(DEFAULT_KEY_SERDE_CLASS_CONFIG,   Serdes.String().getClass().getName());
        streamsConfiguration.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        TopologyTestDriver topologyTestDriver = new TopologyTestDriver(topology, streamsConfiguration);
        TestInputTopic<String, JsonNode> inputTopic = topologyTestDriver
                .createInputTopic("input-topic", new StringSerializer(), JsonNodeSerdes.serdes().serializer());

        String requestId = UUID.randomUUID().toString();
        ObjectMapper mapper = new ObjectMapper();
        VisitEvent visitEvent = VisitEvent.builder()
                .requestId(requestId)
                .apartmentId("apartmentId")
                .visitorId("visitorId")
                .requestStatus(RequestStatus.PENDING)
                .build();
        inputTopic.pipeInput(requestId, mapper.convertValue(visitEvent, JsonNode.class));
        VisitEvent visitEvent1 = VisitEvent.builder()
                .requestId(requestId)
                .apartmentId("apartmentId")
                .visitorId("visitorId")
                .requestStatus(RequestStatus.CANCELLED)
                .build();
        inputTopic.pipeInput(requestId, mapper.convertValue(visitEvent1, JsonNode.class));

        KeyValueStore<String, String> visitEventStream = topologyTestDriver.getKeyValueStore("visit-info");
        KafkaStreams kafkaStreams = mock(KafkaStreams.class);
        when(factoryBean.getKafkaStreams()).thenReturn(kafkaStreams);
        when(kafkaStreams.store(any())).thenReturn(visitEventStream);

        MatcherAssert.assertThat(visitRequestController.getStatus(requestId).getBody().getRequestStatus(),
                equalTo(RequestStatus.CANCELLED.name()));

        MatcherAssert.assertThat(visitRequestController.getStatus("testId").getStatusCode(),
                equalTo(HttpStatus.BAD_REQUEST));

    }

}
