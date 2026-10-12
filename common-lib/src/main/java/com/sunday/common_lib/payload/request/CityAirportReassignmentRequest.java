package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityAirportReassignmentRequest {

    @NotNull(message = ErrorMessageUtil.AIRPORT_CITY_ID_MANDATORY)
    private UUID targetCityId;
}
