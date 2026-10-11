package com.sunday.services.clients;

import java.util.UUID;
import com.sunday.common_lib.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserClientFallback implements UserClient {

    @Override
    public UserDTO getUserById(UUID userId) {
        log.warn("UserClient fallback triggered for userId={}", userId);
        return null;
    }
}
