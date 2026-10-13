package com.sunday.services.client;

import com.sunday.common_lib.dto.PermissionDTO;
import com.sunday.common_lib.dto.RoleDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Deliberately degrades to null for every cause, including a legitimate {@code NotFound} — callers
 * ({@code AirlineMembershipServiceImpl.resolveRole}, {@code OnboardingServiceImpl.resolveOwnerRoleId})
 * already treat "role not found" and "user-service unreachable" the same way (fail closed, 503),
 * since these are internal role lookups, never user-supplied input. This factory only adds cause
 * logging on top of that existing, intentional behavior.
 */
@Slf4j
@Component
public class RoleClientFallbackFactory implements FallbackFactory<RoleClient> {

    @Override
    public RoleClient create(Throwable cause) {
        return new RoleClient() {
            @Override
            public List<PermissionDTO> getPermissionsForRole(UUID roleId) {
                log.warn("user-service call failed – permissions for role {} return null: {}", roleId, cause.getMessage(), cause);
                return null;
            }

            @Override
            public RoleDTO getRoleById(UUID roleId) {
                log.warn("user-service call failed – role {} lookup returns null: {}", roleId, cause.getMessage(), cause);
                return null;
            }

            @Override
            public RoleDTO getRoleByName(String name) {
                log.warn("user-service call failed – role lookup by name {} returns null: {}", name, cause.getMessage(), cause);
                return null;
            }
        };
    }
}
