package com.ams.streams.event;

import com.ams.streams.model.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitEvent {

    private String requestId;

    private String apartmentId;

    private String visitorId;

    private RequestStatus requestStatus;

    private ReplayAttributes replayAttributes;
}
