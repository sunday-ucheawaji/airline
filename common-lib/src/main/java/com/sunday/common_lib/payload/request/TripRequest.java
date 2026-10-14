package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.embeddable.ContactInfo;
import com.sunday.common_lib.enums.TripType;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** A round trip or multi-city journey: the same passengers on every flight, paid with one payment. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripRequest {

    @NotNull(message = ErrorMessageUtil.TRIP_TYPE_MANDATORY)
    private TripType tripType;

    private ContactInfo contactInfo;

    /** Passenger details; their seats are given per flight, in this order. */
    @NotEmpty(message = ErrorMessageUtil.TRIP_PASSENGERS_MANDATORY)
    @Valid
    private List<PassengerRequest> passengers;

    /** The flights in travel order. */
    @NotEmpty(message = ErrorMessageUtil.TRIP_SEGMENTS_MANDATORY)
    @Valid
    private List<TripSegmentRequest> segments;
}
