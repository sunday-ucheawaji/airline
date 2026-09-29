package com.sunday.services.client;

import com.sunday.common_lib.dto.RoleDTO;
import com.sunday.common_lib.payload.response.ReviewerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/** No fallback on purpose: callers must fail closed when user-service cannot answer. */
@FeignClient(name = "user-service", contextId = "userClient")
public interface UserClient {

    @GetMapping("/api/users/{userId}/roles")
    List<RoleDTO> getPlatformRolesForUser(@PathVariable Long userId);

    @GetMapping("/internal/users/by-role/{roleName}")
    List<ReviewerResponse> getReviewersByRole(@PathVariable String roleName);

    /** 404 (surfaced as FeignException.NotFound) when no account has this email. */
    @GetMapping("/internal/users/by-email/{email}")
    ReviewerResponse getUserByEmail(@PathVariable String email);
}
