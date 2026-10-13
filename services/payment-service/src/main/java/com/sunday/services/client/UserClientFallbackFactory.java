package com.sunday.services.client;

import java.util.UUID;
import com.sunday.common_lib.dto.UserDTO;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.exception.UserException;
import com.sunday.common_lib.util.ErrorMessageUtil;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * No call site around {@code getUserById} distinguishes a legitimate "no such user" from an
 * outage today, so the translation has to happen here: a {@code NotFound} cause throws the
 * common-lib exception directly (mapped to 404) instead of degrading to null like a real failure.
 */
@Slf4j
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        return userId -> {
            if (cause instanceof FeignException.NotFound) {
                throw new ResourceNotFoundException(String.format(ErrorMessageUtil.USER_NOT_FOUND_BY_ID, userId));
            }
            log.warn("user-service call failed – user {} lookup returns null: {}", userId, cause.getMessage(), cause);
            return null;
        };
    }
}
