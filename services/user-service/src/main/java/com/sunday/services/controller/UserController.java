package com.sunday.services.controller;

import com.sunday.common_lib.dto.UserDTO;
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
public class        UserController {

    private final UserService userService;

    @GetMapping("/api/users/profile")
    public ResponseEntity<UserDTO> getUserProfile(
            @RequestHeader("X-User-Email") String email) throws UserException {
        User user = userService.getUserByEmail(email);
        return ResponseEntity.ok(UserMapper.toDTO(user));
    }

    @GetMapping("/api/users/{userId}")
    public ResponseEntity<UserDTO> getUserById(
            @PathVariable Long userId) throws UserException {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(UserMapper.toDTO(user));
    }

    @GetMapping("/api/users")
    public ResponseEntity<List<UserDTO>> getUsers() throws UserException {
        List<User> users = userService.getUsers();
        return ResponseEntity.ok(UserMapper.toDTOList(users));
    }
}
