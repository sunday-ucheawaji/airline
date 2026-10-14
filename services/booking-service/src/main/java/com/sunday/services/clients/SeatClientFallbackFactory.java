package com.sunday.services.clients;

import com.sunday.common_lib.payload.response.SeatConfirmResponse;
import com.sunday.common_lib.payload.request.SeatConfirmRequest;
import java.util.UUID;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.payload.request.SeatHoldRequest;
import com.sunday.common_lib.payload.response.SeatHoldResponse;
import com.sunday.common_lib.payload.response.SeatInstanceResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fails closed: a seat outage must not hold a booking's seats at a phantom price or show a
 * booking with no seats. A 409 from seat-service (seat already held/booked, wrong flight
 * instance or cabin) is a legitimate rejection and is passed on with its real message instead of
 * being masked as "unavailable".
 */
@Slf4j
@Component
public class SeatClientFallbackFactory implements FallbackFactory<SeatClient> {

    @Override
    public SeatClient create(Throwable cause) {
        return new SeatClient() {
            @Override
            public SeatHoldResponse holdSeats(SeatHoldRequest request) {
                if (cause instanceof FeignException.Conflict fe) {
                    throw new ConflictException(FeignFailureUtil.extractMessage(fe, "Seat is no longer available"));
                }
                log.warn("seat-service call failed – seat hold could not be completed: {}", cause.getMessage(), cause);
                throw new ServiceUnavailableException(ErrorMessageUtil.SEAT_SERVICE_UNAVAILABLE, cause);
            }

            @Override
            public SeatConfirmResponse confirmSeats(SeatConfirmRequest request) {
                log.warn("seat-service call failed – seats of booking {} could not be confirmed: {}",
                        request.getBookingReference(), cause.getMessage(), cause);
                throw new ServiceUnavailableException(ErrorMessageUtil.SEAT_SERVICE_UNAVAILABLE, cause);
            }

            @Override
            public BigDecimal calculateSeatPrice(List<UUID> seatInstanceIds) {
                log.warn("seat-service call failed – seat premiums could not be priced: {}", cause.getMessage(), cause);
                throw new ServiceUnavailableException(ErrorMessageUtil.SEAT_SERVICE_UNAVAILABLE, cause);
            }

            @Override
            public List<SeatInstanceResponse> getAllByIds(List<UUID> ids) {
                log.warn("seat-service call failed – seat lookup could not be completed: {}", cause.getMessage(), cause);
                throw new ServiceUnavailableException(ErrorMessageUtil.SEAT_SERVICE_UNAVAILABLE, cause);
            }
        };
    }
}
