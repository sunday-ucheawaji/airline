package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.client.UserClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Enforces "a platform user cannot be an airline member" from the airline side, by asking user-service
 * whether a user holds any platform role. Fails closed: if user-service cannot answer, the caller gets a 503.
 */
@Component
@RequiredArgsConstructor
public class PlatformUserGuard {

    private final UserClient userClient;

    /** Throws {@link ConflictException} with {@code conflictMessage} when the user holds a platform role. */
    public void requireNotPlatformUser(Long userId, String conflictMessage) {
        boolean platformUser;
        try {
            platformUser = !userClient.getPlatformRolesForUser(userId).isEmpty();
        } catch (FeignException.NotFound e) {
            throw new BadRequestException(String.format(ErrorMessageUtil.ONBOARDING_OWNER_USER_NOT_FOUND, userId));
        } catch (Exception e) {
            throw new ServiceUnavailableException(ErrorMessageUtil.USER_ROLE_CHECK_UNAVAILABLE, e);
        }
        if (platformUser) {
            throw new ConflictException(conflictMessage);
        }
    }

    /** Throws {@link BadRequestException} unless the user exists and holds the given platform role. */
    public void requireHoldsRole(Long userId, String roleName, String lackingMessage) {
        boolean holds;
        try {
            holds = userClient.getPlatformRolesForUser(userId).stream().anyMatch(role -> roleName.equals(role.getName()));
        } catch (FeignException.NotFound e) {
            throw new BadRequestException(String.format(ErrorMessageUtil.USER_NOT_FOUND_BY_ID, userId));
        } catch (Exception e) {
            throw new ServiceUnavailableException(ErrorMessageUtil.USER_ROLE_CHECK_UNAVAILABLE, e);
        }
        if (!holds) {
            throw new BadRequestException(lackingMessage);
        }
    }
}
