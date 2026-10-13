package com.sunday.services.controller;

import com.sunday.common_lib.dto.AuthUserDTO;
import com.sunday.common_lib.dto.RoleDTO;
import com.sunday.common_lib.payload.request.ChangePasswordRequest;
import com.sunday.common_lib.payload.request.ProfileUpdateRequest;
import com.sunday.common_lib.payload.request.UserStatusUpdateRequest;
import com.sunday.services.enums.UserStatus;
import com.sunday.services.mapper.RoleMapper;
import com.sunday.services.mapper.UserMapper;
import com.sunday.services.model.Role;
import com.sunday.services.model.User;
import com.sunday.services.service.RoleService;
import com.sunday.services.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RoleService roleService;

    @GetMapping("/api/users/profile")
    public ResponseEntity<AuthUserDTO> getUserProfile(@RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(UserMapper.toAuthUserDTO(userService.getUserByEmail(email)));
    }

    @PatchMapping("/api/users/profile")
    public ResponseEntity<AuthUserDTO> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request, @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(UserMapper.toAuthUserDTO(userService.updateProfile(userId, request)));
    }

    @PostMapping("/api/users/profile/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request, @RequestHeader("X-User-Id") UUID userId) {
        userService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/users/{userId}")
    public ResponseEntity<AuthUserDTO> getUserById(@PathVariable UUID userId) {
        return ResponseEntity.ok(UserMapper.toAuthUserDTO(userService.getUserById(userId)));
    }

    @GetMapping("/api/users")
    public ResponseEntity<Page<AuthUserDTO>> getUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.getUsers(pageable).map(UserMapper::toAuthUserDTO));
    }

    @GetMapping("/api/users/{userId}/roles")
    public ResponseEntity<List<RoleDTO>> getPlatformRolesForUser(@PathVariable UUID userId) {
        List<Role> roles = roleService.getPlatformRolesForUser(userId);
        return ResponseEntity.ok(RoleMapper.toDTOList(roles));
    }

    @PutMapping("/api/users/{userId}/status")
    public ResponseEntity<AuthUserDTO> updateStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UserStatusUpdateRequest request,
            @RequestHeader("X-User-Id") UUID actorUserId,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String actorRoles) {
        User user = userService.updateStatus(userId, UserStatus.valueOf(request.getStatus()), actorUserId, parseRoles(actorRoles));
        return ResponseEntity.ok(UserMapper.toAuthUserDTO(user));
    }

    @PostMapping("/api/users/{userId}/unlock")
    public ResponseEntity<AuthUserDTO> unlock(@PathVariable UUID userId) {
        return ResponseEntity.ok(UserMapper.toAuthUserDTO(userService.unlock(userId)));
    }

    private static List<String> parseRoles(String header) {
        return Arrays.stream(header.split(",")).map(String::trim).filter(role -> !role.isEmpty()).toList();
    }
}
