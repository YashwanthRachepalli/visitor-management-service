package com.ams.streams.controller;

import com.ams.streams.model.VisitRequest;
import com.ams.streams.model.VisitRequestStatus;
import com.ams.streams.service.VisitRequestService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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
                StoreQueryParameters.fromNameAndType("visit-info-latest", QueryableStoreTypes.keyValueStore())
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

    @GetMapping
    public ResponseEntity<List<VisitRequestStatus>> getAllVisitRequests() {
        KafkaStreams kafkaStreams = factoryBean.getKafkaStreams();
        ReadOnlyKeyValueStore<String, String> store = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType("visit-info-latest", QueryableStoreTypes.keyValueStore())
        );
        List<VisitRequestStatus> visitRequestStatuses = new ArrayList<>();
        store.all().forEachRemaining(currKeyValue -> visitRequestStatuses
                .add(VisitRequestStatus.builder()
                        .requestStatus(currKeyValue.value)
                        .requestId(currKeyValue.key)
                        .build()));

        return ResponseEntity.ok(visitRequestStatuses);
    }
}
