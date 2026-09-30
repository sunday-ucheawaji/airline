package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.enums.CabinClassType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Per-cabin count of seats already sold/held (status != AVAILABLE) for one flight instance — used by flight-ops-service to validate an aircraft reassignment before committing it. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CabinSeatStatusResponse {
    private CabinClassType cabinClass;
    private long nonAvailableSeatCount;
}
