package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.payload.request.BaggagePolicyRequest;
import lombok.*;

import java.util.List;

/**
 * Partial-success result for a bulk baggage policy create. A row with a data problem (unknown fare, a policy that
 * already exists for the fare, or a second row for the same fare in the same request) is reported in {@code skipped}
 * with its position and a reason. A permission failure on any airline rejects the whole request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaggagePolicyBulkCreateResponse {

    private List<BaggagePolicyResponse> created;
    private List<SkippedRequest> skipped;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkippedRequest {
        private int index;
        private BaggagePolicyRequest request;
        private String reason;
    }
}
