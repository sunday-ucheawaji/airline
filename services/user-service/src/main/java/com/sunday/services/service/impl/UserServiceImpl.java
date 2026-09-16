package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.model.User;
import com.sunday.services.repository.UserRepository;
import com.sunday.services.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

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
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.USER_NOT_FOUND_BY_ID, id)));
    }

    @Override
    public List<User> getUsers() {
        return userRepository.findAll();
    }
}
