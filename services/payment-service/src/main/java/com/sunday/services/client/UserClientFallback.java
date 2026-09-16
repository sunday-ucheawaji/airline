package com.sunday.services.client;

import com.sunday.common_lib.dto.UserDTO;
import com.sunday.common_lib.exception.UserException;
import org.springframework.stereotype.Component;

@Component
public class UserClientFallback implements UserClient {

    @Override
    public UserDTO getUserById(Long userId) throws UserException {
        return null;
    }
}
