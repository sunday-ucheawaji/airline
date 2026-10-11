package com.sunday.services.clients;

import java.util.UUID;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.payload.request.SeatHoldRequest;
import com.sunday.common_lib.payload.response.SeatHoldResponse;
import com.sunday.common_lib.payload.response.SeatInstanceResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import org.springframework.stereotype.Component;

import java.util.List;

/** Fails closed: a seat outage must not hold a booking's seats at a phantom price or show a booking with no seats. */
@Component
public class SeatClientFallback implements SeatClient {

    @Override
    public SeatHoldResponse holdSeats(SeatHoldRequest request) {
        throw new ServiceUnavailableException(ErrorMessageUtil.SEAT_SERVICE_UNAVAILABLE);
    }

    @Override
    public List<SeatInstanceResponse> getAllByIds(List<UUID> ids) {
        throw new ServiceUnavailableException(ErrorMessageUtil.SEAT_SERVICE_UNAVAILABLE);
    }
}
