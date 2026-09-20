package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.embeddable.Support;
import com.sunday.common_lib.enums.AirlineStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AirlineResponse {

    private Long id;

    private String legalName;
    private String name;
    private String alias;
    private String registrationNumber;

    private String iataCode;
    private String icaoCode;

    private String country;

    private String logoUrl;
    private String website;

    private AirlineStatus status;
    private String alliance;

    private Long headquartersCityId;

    private Instant createdAt;
    private Instant updatedAt;

    private Support support;
}
