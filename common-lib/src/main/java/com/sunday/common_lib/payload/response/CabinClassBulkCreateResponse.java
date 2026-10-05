package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.payload.request.CabinClassRequest;
import lombok.*;

import java.util.List;

/**
 * Partial-success result for a bulk create: rows with a data problem (unknown aircraft, a code
 * already used on that aircraft, or a code repeated within the request) are reported in
 * {@code skipped} instead of failing the whole batch. A permission failure on any referenced
 * airline still rejects the whole request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CabinClassBulkCreateResponse {

    private List<CabinClassResponse> created;
    private List<SkippedRequest> skipped;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkippedRequest {
        private CabinClassRequest request;
        private String reason;
    }
}
