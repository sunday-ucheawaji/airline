package com.sunday.services.service;

import com.sunday.services.model.User;

import java.util.List;

public interface UserService {
    User getUserByEmail(String email);
    User getUserById(Long id);
    List<User> getUsers();
}
