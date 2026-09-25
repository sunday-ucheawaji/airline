package com.sunday.services.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** No fallback on purpose: callers must fail closed when airline-core-service cannot answer. */
@FeignClient(name = "airline-core-service")
public interface MembershipClient {

    @GetMapping("/internal/memberships/users/{userId}/exists")
    boolean hasMembership(@PathVariable Long userId);
}
