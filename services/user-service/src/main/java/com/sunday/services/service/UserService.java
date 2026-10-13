package com.sunday.services.service;

import com.sunday.common_lib.payload.request.ChangePasswordRequest;
import com.sunday.common_lib.payload.request.ProfileUpdateRequest;
import com.sunday.services.enums.UserStatus;
import com.sunday.services.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.UUID;

public interface UserService {

    User getUserByEmail(String email);

    User getUserById(UUID id);

    Page<User> getUsers(Pageable pageable);

    User updateProfile(UUID userId, ProfileUpdateRequest request);

    /** Ends every session of the user, including the one making the change. */
    void changePassword(UUID userId, ChangePasswordRequest request);

    User updateStatus(UUID targetUserId, UserStatus status, UUID actorUserId, Collection<String> actorRoles);

    /** Lifts a failed-login lockout before it runs out. */
    User unlock(UUID targetUserId);
}
