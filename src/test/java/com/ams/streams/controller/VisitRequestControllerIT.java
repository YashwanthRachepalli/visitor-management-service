package com.ams.streams.controller;

import com.ams.streams.config.KafkaStreamsConfiguration;
import com.ams.streams.event.VisitEvent;
import com.ams.streams.model.RequestStatus;
import com.ams.streams.model.VisitRequest;
import com.ams.streams.model.VisitRequestStatus;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.equalTo;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { KafkaStreamsConfiguration.class } )
@EmbeddedKafka(controlledShutdown = true, topics = { "visit-event-topic-2" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
public class VisitRequestControllerIT {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTemplate testKafkaTemplate;

    @Autowired
    private KafkaVisitEventListener kafkaVisitEventListener;

    @Value("${topics.visitEventsTopic}")
    private String visitEventTopic;
    private static final String APARTMENT_ID= "apartmentId";
    private static final String VISITOR_ID= "visitorId";

    @Configuration
    static class TestConfig {
        @Bean
        public KafkaVisitEventListener kafkaVisitEventListener() {
            return new KafkaVisitEventListener();
        }
    }


    public static class KafkaVisitEventListener {
        AtomicInteger counter = new AtomicInteger(0);

        @KafkaListener(groupId = "VisitRequestControllerEndToEndIT", topics = "visit-event-topic-2",
                autoStartup = "true", containerFactory = "kafkaListenerContainerFactory")
        void receive(@Payload final List<VisitEvent> payload, @Headers final MessageHeaders headers) {
            log.debug("visit-event-topic-2 - Received message: {}", payload);
            counter.incrementAndGet();
        }
    }


    @Test
    public void test() {
        String id = UUID.randomUUID().toString();
        VisitRequest request = VisitRequest.builder()
                .requestId(Optional.of(id))
                .status(Optional.of(RequestStatus.ACCEPTED))
                .apartmentId(APARTMENT_ID)
                .visitorId(VISITOR_ID)
                .build();

        ResponseEntity<VisitRequestStatus> response = testRestTemplate.exchange(
                "/request",
                HttpMethod.PUT,
                new HttpEntity<VisitRequest>(request),
                VisitRequestStatus.class
        );
        Awaitility.await().atMost(10, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(kafkaVisitEventListener.counter::get, equalTo(1));

        MatcherAssert.assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        MatcherAssert.assertThat(response.getBody().getRequestId(), equalTo(id));
    }

}
