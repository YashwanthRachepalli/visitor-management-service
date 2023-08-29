package com.ams.streams.processor;

import com.ams.streams.event.VisitEvent;
import com.ams.streams.properties.KafkaStreamsProperties;
import com.ams.streams.serdes.JsonNodeSerdes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class VisitRequestTopology {

    @Autowired
    private final KafkaStreamsProperties properties;
    private static final Serde<String> STRING_SERDE = Serdes.String();

    @Autowired
    public void buildPipeline(StreamsBuilder streamsBuilder) {
        KStream<String, JsonNode> messageStream = streamsBuilder
                .stream(properties.getVisitEventsTopic(), Consumed.with(STRING_SERDE, JsonNodeSerdes.serdes()))
                .peek((key, visitEvent) -> log.info("Visit Event received with key: {} and visit event: {}", key,
                        visitEvent));
        messageStream
                .map((key, value) -> {
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.registerModule(new JavaTimeModule());
                    try {
                        VisitEvent visitEvent = mapper.treeToValue(value, VisitEvent.class);
                        return new KeyValue<>(visitEvent.getRequestId(), visitEvent.getRequestStatus().name());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .groupByKey(Grouped.with(STRING_SERDE, STRING_SERDE))
                .reduce((requestStatus, v1) -> v1, Materialized.as("visit-info-latest"));
    }


}
