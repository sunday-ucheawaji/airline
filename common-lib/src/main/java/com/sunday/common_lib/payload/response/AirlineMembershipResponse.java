package com.sunday.common_lib.payload.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AirlineMembershipResponse {

    private UUID id;
    private UUID airlineId;
    private UUID userId;
    private UUID roleId;
    private String status;
    private UUID invitedByUserId;
    private Instant expiresAt;
    private Instant joinedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
