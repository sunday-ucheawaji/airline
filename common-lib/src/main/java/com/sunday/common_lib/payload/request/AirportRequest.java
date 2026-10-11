package com.sunday.common_lib.payload.request;


import com.sunday.common_lib.embeddable.Address;
import com.sunday.common_lib.embeddable.GeoCode;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AirportRequest {

    @NotBlank(message = ErrorMessageUtil.AIRPORT_IATA_CODE_MANDATORY)
    @Size(min = 3, max = 3, message = ErrorMessageUtil.AIRPORT_IATA_CODE_LENGTH)
    private String iataCode;

    @NotBlank(message = ErrorMessageUtil.AIRPORT_NAME_MANDATORY)
    private String name;

    private ZoneId timeZone;

    @Valid
    private Address address;

    @NotNull(message = ErrorMessageUtil.AIRPORT_CITY_ID_MANDATORY)
    private UUID cityId;

    @Valid
    private GeoCode geoCode;
}
