package com.sunday.services.service.impl;

import java.nio.charset.StandardCharsets;
import com.sunday.common_lib.event.EmailVerificationRequestedEvent;
import com.sunday.common_lib.event.PasswordResetRequestedEvent;
import com.sunday.common_lib.exception.UserException;
import com.sunday.common_lib.payload.request.RegisterRequest;
import com.sunday.common_lib.payload.response.AuthResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.config.JwtProvider;
import com.sunday.services.enums.UserStatus;
import com.sunday.services.mapper.UserMapper;
import com.sunday.services.model.EmailVerificationToken;
import com.sunday.services.model.PasswordResetToken;
import com.sunday.services.model.RefreshToken;
import com.sunday.services.model.User;
import com.sunday.services.repository.EmailVerificationTokenRepository;
import com.sunday.services.repository.PasswordResetTokenRepository;
import com.sunday.services.repository.RefreshTokenRepository;
import com.sunday.services.repository.UserRepository;
import com.sunday.services.service.AuthService;
import com.sunday.services.util.PasswordPolicy;
import com.sunday.services.util.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    @Value("${auth.password-reset-token-ttl-minutes:30}")
    private long passwordResetTokenTtlMinutes;

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final ApplicationEventPublisher eventPublisher;
    private final AccessTokenRevoker accessTokenRevoker;

    @Override
    @Transactional
    public AuthResponse signup(RegisterRequest req) {
        PasswordPolicy.requireHashable(req.getPassword());
        String email = normalizeEmail(req.getEmail());
        if (userRepository.findByEmail(email) != null) {
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
        if (user != null) {
            clearExpiredLock(user);
            if (user.getLockedUntil() != null) {
                throw new UserException(ErrorMessageUtil.ACCOUNT_LOCKED);
            }
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new UserException(ErrorMessageUtil.ACCOUNT_NOT_ACTIVE);
            }
        }

        UserDetails userDetails;
        try {
            userDetails = authenticate(email, password);
        } catch (UserException e) {
            if (user != null) {
                registerFailedLogin(user);
            }
            throw e;
        }

        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new UserException(ErrorMessageUtil.EMAIL_NOT_VERIFIED);
        }

        String token = jwtProvider.generateToken(userDetails, user.getId());
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
        EmailVerificationToken verificationToken = emailVerificationTokenRepository
                .findByTokenHash(TokenHasher.sha256Hex(token))
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
        User user = userRepository.findByEmail(normalizeEmail(rawEmail));
        if (user == null || Boolean.TRUE.equals(user.getEmailVerified())) {
            return;
        }
        issueVerificationToken(user);
    }

    /**
     * The old token is revoked with a conditional update, so of two concurrent refreshes with the same token only
     * one can win; the loser is treated as reuse, exactly like presenting an already-rotated token.
     */
    @Override
    @Transactional(noRollbackFor = UserException.class)
    public AuthResponse refresh(String rawRefreshToken, String userAgent, String ipAddress) {
        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(rawRefreshToken))
                .orElseThrow(() -> new UserException(ErrorMessageUtil.INVALID_REFRESH_TOKEN));
        User user = existingToken.getUser();

        if (existingToken.getRevokedAt() != null) {
            rejectReuse(user);
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

        if (refreshTokenRepository.revokeIfActive(existingToken.getId(), LocalDateTime.now()) == 0) {
            rejectReuse(user);
        }

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
        String newJwt = jwtProvider.generateToken(userDetails, user.getId());
        String newRefreshToken = issueRefreshToken(user, userAgent, ipAddress);

        AuthResponse response = new AuthResponse();
        response.setMessage("Token refreshed");
        response.setJwt(newJwt);
        response.setRefreshToken(newRefreshToken);
        response.setUser(UserMapper.toAuthUserDTO(user));
        return response;
    }

    /** Revokes the session's refresh token, and makes the user's current access tokens invalid at the gateway. */
    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(rawRefreshToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(token);
            }
            accessTokenRevoker.revokeAllQuietly(token.getUser().getId());
        });
    }

    @Override
    @Transactional
    public void forgotPassword(String rawEmail) {
        User user = userRepository.findByEmail(normalizeEmail(rawEmail));
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<PasswordResetToken> outstanding = passwordResetTokenRepository.findByUserIdAndUsedAtIsNull(user.getId());
        outstanding.forEach(old -> old.setUsedAt(now));
        passwordResetTokenRepository.saveAll(outstanding);

        String rawToken = TokenHasher.generateRawToken();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(TokenHasher.sha256Hex(rawToken));
        resetToken.setExpiresAt(now.plusMinutes(passwordResetTokenTtlMinutes));
        PasswordResetToken saved = passwordResetTokenRepository.save(resetToken);

        eventPublisher.publishEvent(PasswordResetRequestedEvent.builder()
                .requestId(saved.getId())
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .token(rawToken)
                .expiresAt(saved.getExpiresAt())
                .build());
    }

    /**
     * Proves control of the mailbox, so it also clears a lockout and, if needed, verifies the email. Every session
     * ends: refresh tokens are revoked and existing access tokens stop working at the gateway.
     */
    @Override
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordPolicy.requireHashable(newPassword);
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(TokenHasher.sha256Hex(rawToken))
                .filter(token -> token.getUsedAt() == null && token.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new UserException(ErrorMessageUtil.INVALID_PASSWORD_RESET_TOKEN));

        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setEmailVerified(true);
        userRepository.save(user);

        revokeAllSessions(user.getId());
        accessTokenRevoker.revokeAll(user.getId());
    }

    // ---------- Helpers ----------

    private void rejectReuse(User user) {
        revokeAllSessions(user.getId());
        accessTokenRevoker.revokeAllQuietly(user.getId());
        throw new UserException(ErrorMessageUtil.REFRESH_TOKEN_REUSE_DETECTED);
    }

    /** A lock that has run out starts the count afresh, so one wrong password afterwards does not lock again. */
    private void clearExpiredLock(User user) {
        if (user.getLockedUntil() != null && !user.getLockedUntil().isAfter(LocalDateTime.now())) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }
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

    private void revokeAllSessions(UUID userId) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId);
        LocalDateTime now = LocalDateTime.now();
        activeTokens.forEach(token -> token.setRevokedAt(now));
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
        List<EmailVerificationToken> outstanding = emailVerificationTokenRepository.findByUserIdAndUsedAtIsNull(user.getId());
        LocalDateTime now = LocalDateTime.now();
        outstanding.forEach(old -> old.setUsedAt(now));
        emailVerificationTokenRepository.saveAll(outstanding);

        String rawToken = TokenHasher.generateRawToken();
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUser(user);
        verificationToken.setTokenHash(TokenHasher.sha256Hex(rawToken));
        verificationToken.setExpiresAt(now.plusHours(verificationTokenTtlHours));
        EmailVerificationToken saved = emailVerificationTokenRepository.save(verificationToken);

        eventPublisher.publishEvent(EmailVerificationRequestedEvent.builder()
                .requestId(saved.getId())
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .token(rawToken)
                .expiresAt(saved.getExpiresAt())
                .build());
    }

    private UserDetails authenticate(String email, String password) throws UserException {
        UserDetails userDetails;
        try {
            userDetails = customUserDetailsService.loadUserByUsername(email);
        } catch (UsernameNotFoundException e) {
            passwordEncoder.matches(truncateForBcrypt(password), DUMMY_PASSWORD_HASH);
            throw new UserException(ErrorMessageUtil.INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(truncateForBcrypt(password), userDetails.getPassword())) {
            throw new UserException(ErrorMessageUtil.INVALID_CREDENTIALS);
        }
        return userDetails;
    }

    /** A password longer than BCrypt accepts can never match a stored hash; compare a harmless stand-in instead of throwing. */
    private static String truncateForBcrypt(String password) {
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length <= PasswordPolicy.MAX_BYTES) {
            return password;
        }
        return "";
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
