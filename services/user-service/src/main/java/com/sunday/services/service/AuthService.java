package com.sunday.services.service;

import com.sunday.common_lib.dto.UserDTO;
import com.sunday.common_lib.exception.UserException;
import com.sunday.common_lib.payload.response.AuthResponse;

public interface AuthService {
    AuthResponse login(String email, String password) throws UserException;
    AuthResponse signup(UserDTO req) throws UserException;
}
