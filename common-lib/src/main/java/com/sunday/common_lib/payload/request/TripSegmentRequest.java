package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** One flight of a trip. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSegmentRequest {

    @NotNull(message = ErrorMessageUtil.FLIGHT_INSTANCE_ID_MANDATORY)
    private UUID flightInstanceId;

    @NotNull(message = ErrorMessageUtil.FARE_ID_MANDATORY)
    private UUID fareId;

    @NotNull(message = ErrorMessageUtil.SEARCH_CABIN_CLASS_MANDATORY)
    private CabinClassType cabinClass;

    /** One entry per passenger, in the trip's passenger order; null for a lap infant. */
    private List<UUID> seatInstanceIds;

    private List<UUID> ancillaryIds;
    private List<UUID> mealIds;
}
