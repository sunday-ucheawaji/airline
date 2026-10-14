package com.sunday.services.controller;

import com.sunday.common_lib.payload.request.ResetPasswordRequest;
import com.sunday.common_lib.payload.request.ForgotPasswordRequest;
import com.sunday.common_lib.payload.request.LoginRequest;
import com.sunday.common_lib.payload.request.RegisterRequest;
import com.sunday.common_lib.payload.request.ResendVerificationRequest;
import com.sunday.common_lib.payload.request.VerifyEmailRequest;
import com.sunday.common_lib.exception.AuthError;
import com.sunday.common_lib.exception.UserException;
import com.sunday.common_lib.payload.response.ApiResponse;
import com.sunday.common_lib.payload.response.AuthResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.config.RefreshTokenCookie;
import com.sunday.services.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
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
    private final RefreshTokenCookie refreshTokenCookie;

    /**
     * 202 with the same body whether the email was new or already registered. A concurrent signup with the same
     * email loses on the unique constraint; the winner's verification email covers it, so it gets the same answer.
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> signup(@RequestBody @Valid RegisterRequest req) {
        try {
            authService.signup(req);
        } catch (DataIntegrityViolationException concurrentSignup) {
            // Same answer as any other signup.
        }
        return ResponseEntity.accepted().body(new ApiResponse(ErrorMessageUtil.SIGNUP_ACCEPTED));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody @Valid LoginRequest req, HttpServletRequest request) {
        AuthResponse response = authService.login(
                req.getEmail(), req.getPassword(), userAgentOf(request), clientIpOf(request));
        return withRefreshCookie(response);
    }

    @GetMapping("/verify-email")
    public ApiResponse verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return new ApiResponse(ErrorMessageUtil.EMAIL_VERIFIED);
    }

    @PostMapping("/verify-email")
    public ApiResponse verifyEmail(@RequestBody @Valid VerifyEmailRequest req) {
        authService.verifyEmail(req.getToken());
        return new ApiResponse(ErrorMessageUtil.EMAIL_VERIFIED);
    }

    @PostMapping("/resend-verification")
    public ApiResponse resendVerification(@RequestBody @Valid ResendVerificationRequest req) {
        authService.resendVerification(req.getEmail());
        return new ApiResponse(ErrorMessageUtil.VERIFICATION_RESENT);
    }

    @PostMapping("/forgot-password")
    public ApiResponse forgotPassword(@RequestBody @Valid ForgotPasswordRequest req) {
        authService.forgotPassword(req.getEmail());
        return new ApiResponse(ErrorMessageUtil.PASSWORD_RESET_SENT);
    }

    @PostMapping("/reset-password")
    public ApiResponse resetPassword(@RequestBody @Valid ResetPasswordRequest req) {
        authService.resetPassword(req.getToken(), req.getNewPassword());
        return new ApiResponse(ErrorMessageUtil.PASSWORD_RESET_DONE);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken,
            HttpServletRequest request) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UserException(AuthError.INVALID_REFRESH_TOKEN);
        }
        AuthResponse response = authService.refresh(refreshToken, userAgentOf(request), clientIpOf(request));
        return withRefreshCookie(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear())
                .body(new ApiResponse(ErrorMessageUtil.LOGGED_OUT));
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(AuthResponse response) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.issue(response.getRefreshToken()))
                .body(response);
    }

    private String userAgentOf(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    /**
     * The last X-Forwarded-For entry is the address the api-gateway itself saw and appended; earlier entries are
     * whatever the client sent and cannot be trusted.
     */
    private String clientIpOf(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] hops = forwardedFor.split(",");
            return hops[hops.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
