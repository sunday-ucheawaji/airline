package com.sunday.services.service;
import java.util.UUID;

import com.sunday.services.model.User;

import java.util.List;

public interface UserService {
    User getUserByEmail(String email);
    User getUserById(UUID id);
    List<User> getUsers();
}
