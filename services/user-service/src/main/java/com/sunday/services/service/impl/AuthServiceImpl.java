package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.UserException;
import com.sunday.common_lib.payload.request.RegisterRequest;
import com.sunday.common_lib.payload.response.AuthResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.config.JwtProvider;
import com.sunday.services.enums.UserStatus;
import com.sunday.services.mapper.UserMapper;
import com.sunday.services.model.EmailVerificationToken;
import com.sunday.services.model.RefreshToken;
import com.sunday.services.model.User;
import com.sunday.services.repository.EmailVerificationTokenRepository;
import com.sunday.services.repository.RefreshTokenRepository;
import com.sunday.services.repository.UserRepository;
import com.sunday.services.service.AuthService;
import com.sunday.services.service.EmailService;
import com.sunday.services.util.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @Value("${auth.verification-token-ttl-hours}")
    private long verificationTokenTtlHours;

    @Value("${auth.refresh-token-ttl-days}")
    private long refreshTokenTtlDays;

    @Value("${auth.max-failed-login-attempts}")
    private int maxFailedLoginAttempts;

    @Value("${auth.account-lockout-minutes}")
    private long accountLockoutMinutes;

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final EmailService emailService;


    @Override
    @Transactional
    public AuthResponse signup(RegisterRequest req) {
        String email = normalizeEmail(req.getEmail());

        User existingUser = userRepository.findByEmail(email);
        if (existingUser != null) {
            throw new UserException(ErrorMessageUtil.EMAIL_ALREADY_REGISTERED);
        }

        User createdUser = new User();
        createdUser.setEmail(email);
        createdUser.setPassword(passwordEncoder.encode(req.getPassword()));
        createdUser.setFirstName(req.getFirstName());
        createdUser.setLastName(req.getLastName());
        createdUser.setMiddleName(req.getMiddleName());
        createdUser.setPhoneNumber(req.getPhoneNumber());

        User savedUser;
        try {
            savedUser = userRepository.save(createdUser);
        } catch (DataIntegrityViolationException e) {
            // Loser of a concurrent signup race on the same email.
            throw new UserException(ErrorMessageUtil.EMAIL_ALREADY_REGISTERED);
        }

        issueVerificationToken(savedUser);

        AuthResponse response = new AuthResponse();
        response.setTitle("Welcome " + savedUser.getFirstName() + " " + savedUser.getLastName());
        response.setMessage("Registration successful. Please check your email to verify your account before logging in.");
        response.setUser(UserMapper.toAuthUserDTO(savedUser));
        return response;
    }

    @Override
    @Transactional(noRollbackFor = UserException.class)
    public AuthResponse login(String rawEmail, String password, String userAgent, String ipAddress) {
        String email = normalizeEmail(rawEmail);
        User user = userRepository.findByEmail(email);

        if (user != null && user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new UserException(ErrorMessageUtil.ACCOUNT_LOCKED);
        }
        if (user != null && user.getStatus() != UserStatus.ACTIVE) {
            throw new UserException(ErrorMessageUtil.ACCOUNT_NOT_ACTIVE);
        }

        Authentication authentication;
        try {
            authentication = authenticate(email, password);
        } catch (UserException e) {
            if (user != null) {
                registerFailedLogin(user);
            }
            throw e;
        }

        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new UserException(ErrorMessageUtil.EMAIL_NOT_VERIFIED);
        }

        String token = jwtProvider.generateToken(authentication, user.getId());
        String refreshToken = issueRefreshToken(user, userAgent, ipAddress);

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        AuthResponse response = new AuthResponse();
        response.setTitle("Login successful");
        response.setMessage("Welcome back " + user.getFirstName() + " " + user.getLastName());
        response.setJwt(token);
        response.setRefreshToken(refreshToken);
        response.setUser(UserMapper.toAuthUserDTO(user));
        return response;
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        String tokenHash = TokenHasher.sha256Hex(token);

        EmailVerificationToken verificationToken = emailVerificationTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new UserException(ErrorMessageUtil.INVALID_VERIFICATION_TOKEN));

        if (verificationToken.getUsedAt() != null) {
            throw new UserException(ErrorMessageUtil.VERIFICATION_TOKEN_ALREADY_USED);
        }

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UserException(ErrorMessageUtil.VERIFICATION_TOKEN_EXPIRED);
        }

        verificationToken.setUsedAt(LocalDateTime.now());
        emailVerificationTokenRepository.save(verificationToken);

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resendVerification(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        User user = userRepository.findByEmail(email);
        if (user == null || Boolean.TRUE.equals(user.getEmailVerified())) {
            return;
        }

        issueVerificationToken(user);
    }

    @Override
    @Transactional(noRollbackFor = UserException.class)
    public AuthResponse refresh(String rawRefreshToken, String userAgent, String ipAddress) {
        String tokenHash = TokenHasher.sha256Hex(rawRefreshToken);

        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UserException(ErrorMessageUtil.INVALID_REFRESH_TOKEN));

        User user = existingToken.getUser();

        if (existingToken.getRevokedAt() != null) {
            revokeAllActiveTokensForUser(user.getId());
            throw new UserException(ErrorMessageUtil.REFRESH_TOKEN_REUSE_DETECTED);
        }

        if (existingToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UserException(ErrorMessageUtil.REFRESH_TOKEN_EXPIRED);
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new UserException(ErrorMessageUtil.ACCOUNT_LOCKED);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserException(ErrorMessageUtil.ACCOUNT_NOT_ACTIVE);
        }
        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new UserException(ErrorMessageUtil.EMAIL_NOT_VERIFIED);
        }

        LocalDateTime now = LocalDateTime.now();
        existingToken.setRevokedAt(now);
        existingToken.setLastUsedAt(now);
        refreshTokenRepository.save(existingToken);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null, Collections.emptyList());
        String newJwt = jwtProvider.generateToken(authentication, user.getId());
        String newRefreshToken = issueRefreshToken(user, userAgent, ipAddress);

        AuthResponse response = new AuthResponse();
        response.setMessage("Token refreshed");
        response.setJwt(newJwt);
        response.setRefreshToken(newRefreshToken);
        response.setUser(UserMapper.toAuthUserDTO(user));
        return response;
    }

    @Override
    public void logout(String rawRefreshToken) {
        String tokenHash = TokenHasher.sha256Hex(rawRefreshToken);

        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(token);
            }
        });
    }

    private void registerFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts();
        attempts++;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= maxFailedLoginAttempts) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(accountLockoutMinutes));
        }
        userRepository.save(user);
    }

    private void revokeAllActiveTokensForUser(Long userId) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId);
        LocalDateTime now = LocalDateTime.now();
        for (RefreshToken activeToken : activeTokens) {
            activeToken.setRevokedAt(now);
        }
        refreshTokenRepository.saveAll(activeTokens);
    }

    private String issueRefreshToken(User user, String userAgent, String ipAddress) {
        String rawToken = TokenHasher.generateRawToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(TokenHasher.sha256Hex(rawToken));
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(refreshTokenTtlDays));
        refreshToken.setUserAgent(userAgent);
        refreshToken.setIpAddress(ipAddress);
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    private void issueVerificationToken(User user) {
        List<EmailVerificationToken> outstanding =
                emailVerificationTokenRepository.findByUserIdAndUsedAtIsNull(user.getId());
        LocalDateTime now = LocalDateTime.now();
        for (EmailVerificationToken old : outstanding) {
            old.setUsedAt(now);
        }
        emailVerificationTokenRepository.saveAll(outstanding);

        String rawToken = TokenHasher.generateRawToken();

        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUser(user);
        verificationToken.setTokenHash(TokenHasher.sha256Hex(rawToken));
        verificationToken.setExpiresAt(now.plusHours(verificationTokenTtlHours));
        emailVerificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), rawToken);
    }

    private Authentication authenticate(String email, String password) throws UserException {
        UserDetails userDetails;
        try {
            userDetails = customUserDetailsService.loadUserByUsername(email);
        } catch (UsernameNotFoundException e) {
            passwordEncoder.matches(password, DUMMY_PASSWORD_HASH);
            throw new UserException(ErrorMessageUtil.INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new UserException(ErrorMessageUtil.INVALID_CREDENTIALS);
        }
        return new UsernamePasswordAuthenticationToken(
                email, null, userDetails.getAuthorities());
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
