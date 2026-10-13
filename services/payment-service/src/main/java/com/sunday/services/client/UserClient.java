package com.sunday.services.client;

import java.util.UUID;
import com.sunday.common_lib.dto.UserDTO;
import com.sunday.common_lib.exception.UserException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {

    @GetMapping("/api/users/{userId}")
    UserDTO getUserById(
            @PathVariable UUID userId) throws UserException;
}
