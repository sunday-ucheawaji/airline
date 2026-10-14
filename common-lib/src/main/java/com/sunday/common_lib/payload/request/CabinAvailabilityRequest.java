package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.CabinClassType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CabinAvailabilityRequest {

    @NotNull(message = "Cabin class is required")
    private CabinClassType cabinClass;

    @NotEmpty(message = "At least one flight instance is required")
    @Size(max = 500, message = "At most 500 flight instances can be checked at once")
    private List<UUID> flightInstanceIds;
}
