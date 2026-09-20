package com.sunday.common_lib.payload.response;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AirlineMembershipResponse {

    private Long id;
    private Long airlineId;
    private Long userId;
    private Long roleId;
    private String status;
    private Instant joinedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
