package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.UserException;
import com.sunday.common_lib.payload.request.RegisterRequest;
import com.sunday.common_lib.payload.response.AuthResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.config.JwtProvider;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    @Value("${auth.verification-token-ttl-hours}")
    private long verificationTokenTtlHours;

    @Value("${auth.refresh-token-ttl-days}")
    private long refreshTokenTtlDays;

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final EmailService emailService;

    /*
    Steps:
        1. Check if email already exists
        2. Encode password using BCrypt
        3. Save user in database
        4. Generate an email verification token and send it
        5. Generate access + refresh tokens
        6. Return tokens and user information
    */
    @Override
    @Transactional
    public AuthResponse signup(RegisterRequest req) throws UserException {
        User existingUser = userRepository.findByEmail(req.getEmail());
        if (existingUser != null) {
            throw new UserException(ErrorMessageUtil.EMAIL_ALREADY_REGISTERED);
        }

        User createdUser = new User();
        createdUser.setEmail(req.getEmail());
        createdUser.setPassword(passwordEncoder.encode(req.getPassword()));
        createdUser.setFirstName(req.getFirstName());
        createdUser.setLastName(req.getLastName());
        createdUser.setMiddleName(req.getMiddleName());
        createdUser.setPhoneNumber(req.getPhoneNumber());
        createdUser.setLastLogin(LocalDateTime.now());

        User savedUser = userRepository.save(createdUser);

        issueVerificationToken(savedUser);

        Authentication authentication
                = new UsernamePasswordAuthenticationToken(
                savedUser.getEmail(), savedUser.getPassword()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtProvider.generateToken(authentication, savedUser.getId());
        String refreshToken = issueRefreshToken(savedUser);

        AuthResponse response = new AuthResponse();
        response.setTitle("Welcome " + savedUser.getFirstName() + " " + savedUser.getLastName());
        response.setMessage("Registration successful. Please check your email to verify your account.");
        response.setUser(UserMapper.toAuthUserDTO(savedUser));
        response.setJwt(jwt);
        response.setRefreshToken(refreshToken);
        return response;
    }

    /*
    Steps:
        1. Load user by email
        2. Compare password with BCrypt
        3. Update `lastLogin` time
        4. Generate access + refresh tokens
        5. Return tokens and user information
    */
    @Override
    @Transactional
    public AuthResponse login(String email, String password) throws UserException {
        Authentication authentication = authenticate(email, password);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(email);
        String token = jwtProvider.generateToken(authentication, user.getId());
        String refreshToken = issueRefreshToken(user);

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
    public void verifyEmail(String token) throws UserException {
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
    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null || Boolean.TRUE.equals(user.getEmailVerified())) {
            return;
        }

        issueVerificationToken(user);
    }

    /*
    Steps:
        1. Hash the submitted refresh token and look it up
        2. If it was already revoked, treat this as token reuse/theft and kill every
           active session for that user, then reject
        3. If it's expired, reject
        4. Rotate: revoke the presented token, issue a brand-new access + refresh pair
    */
    @Override
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) throws UserException {
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

        LocalDateTime now = LocalDateTime.now();
        existingToken.setRevokedAt(now);
        existingToken.setLastUsedAt(now);
        refreshTokenRepository.save(existingToken);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null, Collections.emptyList());
        String newJwt = jwtProvider.generateToken(authentication, user.getId());
        String newRefreshToken = issueRefreshToken(user);

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

    private void revokeAllActiveTokensForUser(Long userId) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId);
        LocalDateTime now = LocalDateTime.now();
        for (RefreshToken activeToken : activeTokens) {
            activeToken.setRevokedAt(now);
        }
        refreshTokenRepository.saveAll(activeTokens);
    }

    private String issueRefreshToken(User user) {
        String rawToken = TokenHasher.generateRawToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(TokenHasher.sha256Hex(rawToken));
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(refreshTokenTtlDays));
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    private void issueVerificationToken(User user) {
        String rawToken = TokenHasher.generateRawToken();

        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUser(user);
        verificationToken.setTokenHash(TokenHasher.sha256Hex(rawToken));
        verificationToken.setExpiresAt(LocalDateTime.now().plusHours(verificationTokenTtlHours));
        emailVerificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), rawToken);
    }

    private Authentication authenticate(String email, String password) throws UserException {
        UserDetails userDetails = customUserDetailsService
                .loadUserByUsername(email);
        if (userDetails == null) {
            throw new UserException(String.format(ErrorMessageUtil.USER_NOT_FOUND_BY_EMAIL, email));
        }
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new UserException(ErrorMessageUtil.INVALID_PASSWORD);
        }
        return new UsernamePasswordAuthenticationToken(
                email, null, userDetails.getAuthorities());
    }
}
