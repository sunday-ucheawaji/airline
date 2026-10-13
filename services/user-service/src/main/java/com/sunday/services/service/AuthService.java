package com.sunday.services.service;

import com.sunday.common_lib.payload.request.RegisterRequest;
import com.sunday.common_lib.payload.response.AuthResponse;

public interface AuthService {

    AuthResponse login(String email, String password, String userAgent, String ipAddress);

    AuthResponse signup(RegisterRequest req);

    void verifyEmail(String token);

    void resendVerification(String email);

    AuthResponse refresh(String refreshToken, String userAgent, String ipAddress);

    void logout(String refreshToken);

    /** Always succeeds from the caller's point of view, so it never reveals whether an account exists. */
    void forgotPassword(String email);

    void resetPassword(String token, String newPassword);
}
