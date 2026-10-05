package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.payload.request.SeatMapRequest;
import lombok.*;

import java.util.List;

/**
 * Partial-success result for a bulk create: rows with a data problem (unknown cabin class or
 * aircraft, or a cabin class that already has a seat map or appears twice in the request) are
 * reported in {@code skipped} instead of failing the whole batch. A permission failure on any
 * referenced airline still rejects the whole request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatMapBulkCreateResponse {

    private List<SeatMapResponse> created;
    private List<SkippedRequest> skipped;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkippedRequest {
        private SeatMapRequest request;
        private String reason;
    }
}
