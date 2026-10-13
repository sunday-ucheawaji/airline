package com.sunday.common_lib.payload.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentAccessLogResponse {

    private UUID id;
    private UUID documentId;
    private UUID applicationId;
    private UUID userId;
    private String accessor;
    private Instant accessedAt;
}
