package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.payload.request.InsuranceCoverageRequest;
import lombok.*;

import java.util.List;

/**
 * Partial-success result for a bulk create: rows with a genuine data problem (unknown
 * ancillaryId) are reported in {@code skipped} instead of failing the whole batch. Authorization
 * is not a per-item concern here — the caller must have {@code ANCILLARY_INSURANCE_MANAGE} on
 * every distinct airline referenced before any row is inserted, so a permission failure rejects
 * the whole request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceCoverageBulkCreateResponse {

    private List<InsuranceCoverageResponse> created;
    private List<SkippedRequest> skipped;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkippedRequest {
        private InsuranceCoverageRequest request;
        private String reason;
    }
}
