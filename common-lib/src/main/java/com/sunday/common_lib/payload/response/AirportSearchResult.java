package com.sunday.common_lib.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AirportSearchResult {

    private UUID id;
    private String iataCode;
    private String name;
    private String cityName;
    private String cityCode;
    private String countryCode;
    private String countryName;
    private String timeZoneId;
}
