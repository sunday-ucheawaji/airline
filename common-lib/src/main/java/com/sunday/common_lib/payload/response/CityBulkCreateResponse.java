package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.payload.request.CityRequest;
import lombok.*;

import java.util.List;

/**
 * Partial-success result for a bulk create: rows with a data problem (an invalid code format, a code
 * already used, or a code repeated within the request) are reported in {@code skipped} instead of
 * failing the whole batch.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityBulkCreateResponse {

    private List<CityResponse> created;
    private List<SkippedRequest> skipped;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkippedRequest {
        private CityRequest request;
        private String reason;
    }
}
