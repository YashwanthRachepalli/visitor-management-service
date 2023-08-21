package com.ams.streams.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.ObjectUtils;

import java.util.Optional;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitRequest {

    private Optional<String> requestId;
    private String apartmentId;
    private String visitorId;
    private Optional<RequestStatus> status;

    public Optional<String> getRequestId() {
        if (ObjectUtils.isEmpty(requestId)) {
            return Optional.empty();
        }
        return requestId;
    }

    public Optional<RequestStatus> getStatus() {
        if (ObjectUtils.isEmpty(status)) {
            return Optional.empty();
        }
        return status;
    }



}
