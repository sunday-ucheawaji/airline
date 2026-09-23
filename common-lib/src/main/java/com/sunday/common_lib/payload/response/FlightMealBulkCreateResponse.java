package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.payload.request.FlightMealRequest;
import lombok.*;

import java.util.List;

/**
 * Partial-success result for a bulk create: rows with a genuine data problem (unknown mealId, or
 * a meal already assigned to that flight) are reported in {@code skipped} instead of failing the
 * whole batch. Authorization is not a per-item concern here — the caller must have
 * {@code ANCILLARY_MANAGE} on every distinct airline referenced before any row is inserted, so a
 * permission failure rejects the whole request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightMealBulkCreateResponse {

    private List<FlightMealResponse> created;
    private List<SkippedRequest> skipped;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkippedRequest {
        private FlightMealRequest request;
        private String reason;
    }
}
