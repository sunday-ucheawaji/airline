package com.sunday.services.controller;

import com.sunday.common_lib.exception.UserException;
import com.sunday.common_lib.payload.request.LoginRequest;
import com.sunday.common_lib.payload.request.RefreshTokenRequest;
import com.sunday.common_lib.payload.request.RegisterRequest;
import com.sunday.common_lib.payload.request.ResendVerificationRequest;
import com.sunday.common_lib.payload.response.AuthResponse;
import com.sunday.services.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(
            @RequestBody @Valid RegisterRequest req) throws UserException {
        AuthResponse response = authService.signup(req);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody @Valid LoginRequest req) throws UserException {
        AuthResponse response = authService.login(req.getEmail(), req.getPassword());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(
            @RequestParam String token) throws UserException {
        authService.verifyEmail(token);
        return ResponseEntity.ok("Email verified successfully");
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerification(
            @RequestBody @Valid ResendVerificationRequest req) {
        authService.resendVerification(req.getEmail());
        return ResponseEntity.ok("If that email is registered and not yet verified, a verification link has been sent");
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody @Valid RefreshTokenRequest req) throws UserException {
        AuthResponse response = authService.refresh(req.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @RequestBody @Valid RefreshTokenRequest req) {
        authService.logout(req.getRefreshToken());
        return ResponseEntity.ok("Logged out successfully");
    }
}
