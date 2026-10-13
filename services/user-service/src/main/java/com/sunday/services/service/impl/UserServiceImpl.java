package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.PlatformRoles;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.OperationNotPermittedException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.exception.UserException;
import com.sunday.common_lib.payload.request.ChangePasswordRequest;
import com.sunday.common_lib.payload.request.ProfileUpdateRequest;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.enums.UserStatus;
import com.sunday.services.model.RefreshToken;
import com.sunday.services.model.User;
import com.sunday.services.repository.RefreshTokenRepository;
import com.sunday.services.repository.UserPlatformRoleRepository;
import com.sunday.services.repository.UserRepository;
import com.sunday.services.service.UserService;
import com.sunday.services.util.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserPlatformRoleRepository userPlatformRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenRevoker accessTokenRevoker;

    @Override
    public User getUserByEmail(String email) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail);
        if (user == null) {
            throw new ResourceNotFoundException(String.format(ErrorMessageUtil.USER_NOT_FOUND_BY_EMAIL, email));
        }
        return user;
    }

    @Override
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.USER_NOT_FOUND_BY_ID, id)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public User updateProfile(UUID userId, ProfileUpdateRequest request) {
        User user = getUserById(userId);
        if (request.getFirstName() != null) {
            user.setFirstName(requireText(request.getFirstName()));
        }
        if (request.getLastName() != null) {
            user.setLastName(requireText(request.getLastName()));
        }
        if (request.getMiddleName() != null) {
            user.setMiddleName(StringUtils.hasText(request.getMiddleName()) ? request.getMiddleName().strip() : null);
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(StringUtils.hasText(request.getPhoneNumber()) ? request.getPhoneNumber().strip() : null);
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        PasswordPolicy.requireHashable(request.getNewPassword());
        User user = getUserById(userId);
        if (!matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UserException(ErrorMessageUtil.CURRENT_PASSWORD_INCORRECT);
        }
        if (matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException(ErrorMessageUtil.PASSWORD_UNCHANGED);
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        endAllSessions(userId);
    }

    /**
     * The gateway checks USER_DISABLE; this enforces the rules: never your own account, only a super admin acts on
     * a super admin, and the last active super admin is never suspended. Suspension ends every session at once.
     */
    @Override
    @Transactional
    public User updateStatus(UUID targetUserId, UserStatus status, UUID actorUserId, Collection<String> actorRoles) {
        if (targetUserId.equals(actorUserId)) {
            throw new OperationNotPermittedException(ErrorMessageUtil.USER_STATUS_SELF_CHANGE_FORBIDDEN);
        }
        User user = getUserById(targetUserId);
        boolean targetIsSuperAdmin = userPlatformRoleRepository.findByUserId(targetUserId).stream()
                .anyMatch(grant -> PlatformRoles.SUPER_ADMIN.equals(grant.getRole().getName()));
        if (targetIsSuperAdmin && !actorRoles.contains(PlatformRoles.SUPER_ADMIN)) {
            throw new OperationNotPermittedException(ErrorMessageUtil.USER_STATUS_SUPER_ADMIN_ONLY);
        }
        if (targetIsSuperAdmin && status != UserStatus.ACTIVE && user.getStatus() == UserStatus.ACTIVE
                && userPlatformRoleRepository.countByRoleNameAndUserStatus(PlatformRoles.SUPER_ADMIN, UserStatus.ACTIVE) <= 1) {
            throw new ConflictException(ErrorMessageUtil.LAST_SUPER_ADMIN);
        }
        user.setStatus(status);
        User saved = userRepository.save(user);
        if (status != UserStatus.ACTIVE) {
            endAllSessions(targetUserId);
        }
        return saved;
    }

    @Override
    @Transactional
    public User unlock(UUID targetUserId) {
        User user = getUserById(targetUserId);
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        return userRepository.save(user);
    }

    private void endAllSessions(UUID userId) {
        List<RefreshToken> active = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId);
        LocalDateTime now = LocalDateTime.now();
        active.forEach(token -> token.setRevokedAt(now));
        refreshTokenRepository.saveAll(active);
        accessTokenRevoker.revokeAll(userId);
    }

    /** A password longer than BCrypt accepts can never be the stored one. */
    private boolean matches(String raw, String hash) {
        if (raw == null || raw.getBytes(StandardCharsets.UTF_8).length > PasswordPolicy.MAX_BYTES) {
            return false;
        }
        return passwordEncoder.matches(raw, hash);
    }

    private static String requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(ErrorMessageUtil.NAME_MANDATORY);
        }
        return value.strip();
    }
}
