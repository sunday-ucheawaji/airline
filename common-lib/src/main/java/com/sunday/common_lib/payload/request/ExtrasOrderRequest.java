package com.sunday.common_lib.payload.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Extras bought for a confirmed booking: offerings on its flight and cabin; a repeated id means "buy N". */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtrasOrderRequest {

    private List<UUID> ancillaryIds;
    private List<UUID> mealIds;
}
