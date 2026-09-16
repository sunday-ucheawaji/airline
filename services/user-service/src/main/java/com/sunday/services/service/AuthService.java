package com.sunday.services.service;

import com.sunday.common_lib.exception.UserException;
import com.sunday.common_lib.payload.request.RegisterRequest;
import com.sunday.common_lib.payload.response.AuthResponse;

public interface AuthService {
    AuthResponse login(String email, String password) throws UserException;
    AuthResponse signup(RegisterRequest req) throws UserException;
    void verifyEmail(String token) throws UserException;
    void resendVerification(String email);
    AuthResponse refresh(String refreshToken) throws UserException;
    void logout(String refreshToken);
}
