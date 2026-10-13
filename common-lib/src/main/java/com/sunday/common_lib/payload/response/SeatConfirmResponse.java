package com.sunday.common_lib.payload.response;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatConfirmResponse {

    /** False when the cabin had no free seat left for a lost one; nothing was booked and the held seats were released. */
    private boolean allocated;
    /** The booked seats, in the same order as requested, with replacements in place of lost seats. */
    private List<UUID> seatInstanceIds;
    /** Lost seat -> the seat that replaced it. Empty when every chosen seat was still held. */
    private Map<UUID, UUID> reassigned;
}
