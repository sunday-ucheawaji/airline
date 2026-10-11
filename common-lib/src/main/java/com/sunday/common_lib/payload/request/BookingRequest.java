package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.embeddable.ContactInfo;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.enums.TripType;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    @NotNull(message = ErrorMessageUtil.FLIGHT_ID_MANDATORY)
    private UUID flightId;

    @NotNull(message = ErrorMessageUtil.FLIGHT_INSTANCE_ID_MANDATORY)
    private UUID flightInstanceId;

    @NotNull(message = ErrorMessageUtil.SEARCH_CABIN_CLASS_MANDATORY)
    private CabinClassType cabinClass;

    @NotNull(message = ErrorMessageUtil.TRIP_TYPE_MANDATORY)
    private TripType tripType;

    @NotNull(message = ErrorMessageUtil.FARE_ID_MANDATORY)
    private UUID fareId;

    @NotNull(message = ErrorMessageUtil.AT_LEAST_ONE_PASSENGER_REQUIRED)
    @Size(min = 1, message = ErrorMessageUtil.AT_LEAST_ONE_PASSENGER_REQUIRED)
    private List<PassengerRequest> passengers;

    private ContactInfo contactInfo;

    private List<UUID> ancillaryIds;
    private List<UUID> mealIds;

    private String promoCode;

    private List<String> seatNumbers;
}
