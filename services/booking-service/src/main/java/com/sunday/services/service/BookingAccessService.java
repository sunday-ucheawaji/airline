package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.exception.OperationNotPermittedException;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.clients.AirlineClient;
import com.sunday.services.model.Booking;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The one place booking-service decides who may act on a booking. The owner of a booking is its customer; everyone
 * else must hold the named permission on the booking's airline, checked live through airline-core-service.
 */
@Service
@RequiredArgsConstructor
public class BookingAccessService {

    private final AirlineClient airlineClient;

    public void requireOwnerOrPermission(Booking booking, UUID userId, String permission) {
        if (isOwner(booking, userId)) {
            return;
        }
        requireAirlinePermission(booking.getAirlineId(), userId, permission);
    }

    public boolean isOwner(Booking booking, UUID userId) {
        return userId != null && userId.equals(booking.getUserId());
    }

    public void requireAirlinePermission(UUID airlineId, UUID userId, String permission) {
        if (!hasAirlinePermission(airlineId, userId, permission)) {
            throw new OperationNotPermittedException(String.format(ErrorMessageUtil.NO_PERMISSION_FOR_AIRLINE, permission, airlineId));
        }
    }

    /** A caller with no membership on the airline holds no permissions there, so airline-core's 403 means "none". */
    public boolean hasAirlinePermission(UUID airlineId, UUID userId, String permission) {
        List<String> granted;
        try {
            granted = airlineClient.getMyPermissions(airlineId, userId);
        } catch (RuntimeException e) {
            // The Feign client runs inside a circuit breaker with no fallback, so Resilience4j wraps the
            // FeignException (a 403 arrives as NoFallbackAvailableException). Look for it in the cause chain.
            FeignException feign = feignCause(e);
            if (feign instanceof FeignException.Forbidden) {
                return false;
            }
            throw new ServiceUnavailableException(ErrorMessageUtil.AIRLINE_SERVICE_UNAVAILABLE, e);
        }
        if (granted == null) {
            throw new ServiceUnavailableException(ErrorMessageUtil.AIRLINE_SERVICE_UNAVAILABLE);
        }
        return granted.contains(permission);
    }

    private static FeignException feignCause(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof FeignException feign) {
                return feign;
            }
        }
        return null;
    }
}
