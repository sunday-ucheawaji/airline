package com.sunday.services.service;

import com.sunday.common_lib.exception.OperationNotPermittedException;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.client.AirlineClient;
import com.sunday.services.model.Payment;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The one place payment-service decides who may act on a payment. The owner is its payer; everyone
 * else must hold the named permission on the payment's (denormalized) airlineId, checked live through
 * airline-core-service. Mirrors booking-service's BookingAccessService exactly.
 */
@Service
@RequiredArgsConstructor
public class PaymentAccessService {

    private final AirlineClient airlineClient;

    public void requireOwnerOrPermission(Payment payment, Long userId, String permission) {
        if (isOwner(payment, userId)) {
            return;
        }
        requireAirlinePermission(payment.getAirlineId(), userId, permission);
    }

    public boolean isOwner(Payment payment, Long userId) {
        return userId != null && userId.equals(payment.getUserId());
    }

    public void requireAirlinePermission(Long airlineId, Long userId, String permission) {
        if (!hasAirlinePermission(airlineId, userId, permission)) {
            throw new OperationNotPermittedException(String.format(ErrorMessageUtil.NO_PERMISSION_FOR_AIRLINE, permission, airlineId));
        }
    }

    /** A caller with no membership on the airline holds no permissions there, so airline-core's 403 means "none". */
    public boolean hasAirlinePermission(Long airlineId, Long userId, String permission) {
        List<String> granted;
        try {
            granted = airlineClient.getMyPermissions(airlineId, userId);
        } catch (RuntimeException e) {
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
