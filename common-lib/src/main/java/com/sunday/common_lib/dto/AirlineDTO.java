package com.sunday.common_lib.dto;

import com.sunday.common_lib.embeddable.Support;
import com.sunday.common_lib.enums.AirlineStatus;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class AirlineDTO {

    private UUID id;

    private String iataCode;
    private String icaoCode;

    private String name;
    private String alias;
    private String country;

    private String logoUrl;
    private String website;

    private AirlineStatus status;
    private String alliance;

    private Support support;
    private String headquarters;

    private Instant createdAt;
    private Instant updatedAt;

    private UUID baggagePolicyId;

    private UUID userId;
    private UUID updatedByUserId;
}
