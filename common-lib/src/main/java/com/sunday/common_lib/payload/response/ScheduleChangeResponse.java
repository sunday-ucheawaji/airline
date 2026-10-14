package com.sunday.common_lib.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The result of editing or deleting a schedule: only future departures without sales follow the change; departures
 * that already have seats sold or held are left exactly as they were and counted here, so staff can handle them.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleChangeResponse {

    /** The schedule after an edit; null after a delete. */
    private FlightScheduleResponse schedule;
    private int departuresRemoved;
    private int departuresCreated;
    private int departuresKeptWithSales;
}
