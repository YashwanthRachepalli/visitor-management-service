package com.ams.streams.processor;

import static org.apache.kafka.streams.StreamsConfig.*;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import com.ams.streams.event.VisitEvent;
import com.ams.streams.model.RequestStatus;
import com.ams.streams.properties.KafkaStreamsProperties;
import com.ams.streams.serdes.JsonNodeSerdes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.state.KeyValueStore;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Properties;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class VisitRequestTopologyTest {

    @Mock
    KafkaStreamsProperties kafkaStreamsProperties;

    @InjectMocks
    VisitRequestTopology visitRequestTopology;

    private final String VISIT_REQUEST_INBOUND_TOPIC = "test-visit-topic";
    private static final String APARTMENT_ID= "apartmentId";
    private static final String VISITOR_ID= "visitorId";

    @Test
    public void testVisitRequestTopology() {
        when(kafkaStreamsProperties.getVisitEventsTopic()).thenReturn(VISIT_REQUEST_INBOUND_TOPIC);
        StreamsBuilder streamsBuilder = new StreamsBuilder();
        visitRequestTopology.buildPipeline(streamsBuilder);
        Topology topology = streamsBuilder.build();

        Properties streamsConfiguration = new Properties();
        streamsConfiguration.put(APPLICATION_ID_CONFIG, "visitor-service");
        streamsConfiguration.put(DEFAULT_KEY_SERDE_CLASS_CONFIG,   Serdes.String().getClass().getName());
        streamsConfiguration.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        TopologyTestDriver topologyTestDriver = new TopologyTestDriver(topology, streamsConfiguration);
        TestInputTopic<String, JsonNode> inputTopic = topologyTestDriver
                .createInputTopic(VISIT_REQUEST_INBOUND_TOPIC, new StringSerializer(), JsonNodeSerdes.serdes().serializer());

        String requestId = UUID.randomUUID().toString();
        ObjectMapper mapper = new ObjectMapper();
        VisitEvent visitEvent = VisitEvent.builder()
                .requestId(requestId)
                .apartmentId(APARTMENT_ID)
                .visitorId(VISITOR_ID)
                .requestStatus(RequestStatus.PENDING)
                .build();
        inputTopic.pipeInput(requestId, mapper.convertValue(visitEvent, JsonNode.class));
        VisitEvent visitEvent1 = VisitEvent.builder()
                .requestId(requestId)
                .apartmentId(APARTMENT_ID)
                .visitorId(VISITOR_ID)
                .requestStatus(RequestStatus.CANCELLED)
                .build();
        inputTopic.pipeInput(requestId, mapper.convertValue(visitEvent1, JsonNode.class));

        KeyValueStore<String, String> visitEventStream = topologyTestDriver
                .getKeyValueStore("visit-info-latest");
        MatcherAssert.assertThat(visitEventStream.get(requestId), equalTo(RequestStatus.CANCELLED.name()));
    }
}
