package com.sunday.services.controller;

import com.sunday.common_lib.dto.AuthUserDTO;
import com.sunday.common_lib.exception.UserException;
import com.sunday.services.mapper.UserMapper;
import com.sunday.services.model.User;
import com.sunday.services.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/api/users/profile")
    public ResponseEntity<AuthUserDTO> getUserProfile(
            @RequestHeader("X-User-Email") String email) throws UserException {
        User user = userService.getUserByEmail(email);
        return ResponseEntity.ok(UserMapper.toAuthUserDTO(user));
    }

    @GetMapping("/api/users/{userId}")
    public ResponseEntity<AuthUserDTO> getUserById(
            @PathVariable Long userId) throws UserException {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(UserMapper.toAuthUserDTO(user));
    }

    @GetMapping("/api/users")
    public ResponseEntity<List<AuthUserDTO>> getUsers() throws UserException {
        List<User> users = userService.getUsers();
        return ResponseEntity.ok(UserMapper.toAuthUserDTOList(users));
    }
}
