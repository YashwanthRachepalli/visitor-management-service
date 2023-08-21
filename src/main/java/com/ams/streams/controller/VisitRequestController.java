package com.ams.streams.controller;

import com.ams.streams.event.VisitEvent;
import com.ams.streams.model.RequestStatus;
import com.ams.streams.model.VisitRequest;
import com.ams.streams.model.VisitRequestStatus;
import com.ams.streams.properties.KafkaStreamsProperties;
import com.ams.streams.service.VisitRequestService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/request")
public class VisitRequestController {

    @Autowired
    private StreamsBuilderFactoryBean factoryBean;

    @Autowired
    VisitRequestService visitRequestService;

    @PutMapping
    public ResponseEntity<VisitRequestStatus> saveVisitRequest(@RequestBody VisitRequest request) {
         VisitRequestStatus response = visitRequestService.createVisitRequest(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{requestId}")
    public ResponseEntity<VisitRequestStatus> getStatus(@PathVariable String requestId) {

        KafkaStreams kafkaStreams = factoryBean.getKafkaStreams();
        ReadOnlyKeyValueStore<String, String> store = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType("visit-info", QueryableStoreTypes.keyValueStore())
        );

        if (ObjectUtils.isEmpty(store.get(requestId))) {
            log.info("Request Id: {} not found!", requestId);
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(VisitRequestStatus.builder()
                .requestId(requestId)
                .requestStatus(store.get(requestId))
                .build());
    }
}
