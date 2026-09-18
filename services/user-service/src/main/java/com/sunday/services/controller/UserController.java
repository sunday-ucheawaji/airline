package com.sunday.services.controller;

import com.sunday.common_lib.dto.AuthUserDTO;
import com.sunday.common_lib.dto.RoleDTO;
import com.sunday.services.mapper.RoleMapper;
import com.sunday.services.mapper.UserMapper;
import com.sunday.services.model.Role;
import com.sunday.services.model.User;
import com.sunday.services.service.RoleService;
import com.sunday.services.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RoleService roleService;

    @GetMapping("/api/users/profile")
    public ResponseEntity<AuthUserDTO> getUserProfile(
            @RequestHeader("X-User-Email") String email) {
        User user = userService.getUserByEmail(email);
        return ResponseEntity.ok(UserMapper.toAuthUserDTO(user));
    }

    @GetMapping("/api/users/{userId}")
    public ResponseEntity<AuthUserDTO> getUserById(
            @PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(UserMapper.toAuthUserDTO(user));
    }

    @GetMapping("/api/users")
    public ResponseEntity<List<AuthUserDTO>> getUsers() {
        List<User> users = userService.getUsers();
        return ResponseEntity.ok(UserMapper.toAuthUserDTOList(users));
    }

    @GetMapping("/api/users/{userId}/roles")
    public ResponseEntity<List<RoleDTO>> getPlatformRolesForUser(
            @PathVariable Long userId) {
        List<Role> roles = roleService.getPlatformRolesForUser(userId);
        return ResponseEntity.ok(RoleMapper.toDTOList(roles));
    }
}
