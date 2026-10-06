package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.payload.request.FareRequest;
import lombok.*;

import java.util.List;

/**
 * Partial-success result for a bulk fare create. A row with a data problem (invalid window or currency, an unknown
 * flight, a fare that already exists, or a duplicate of an earlier row in the same request) is reported in
 * {@code skipped} with its position and a reason. A permission failure on any airline rejects the whole request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareBulkCreateResponse {

    private List<FareResponse> created;
    private List<SkippedRequest> skipped;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkippedRequest {
        private int index;
        private FareRequest request;
        private String reason;
    }
}
