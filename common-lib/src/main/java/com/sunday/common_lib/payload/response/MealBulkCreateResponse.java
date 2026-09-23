package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.payload.request.MealRequest;
import lombok.*;

import java.util.List;

/**
 * Partial-success result for a bulk create: rows with a genuine data problem (missing airlineId,
 * or a code already used by that airline) are reported in {@code skipped} instead of failing the
 * whole batch. Authorization is not a per-item concern here — the caller must have
 * {@code ANCILLARY_MANAGE} on every distinct airline referenced before any row is inserted, so a
 * permission failure rejects the whole request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealBulkCreateResponse {

    private List<MealResponse> created;
    private List<SkippedRequest> skipped;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkippedRequest {
        private MealRequest request;
        private String reason;
    }
}
