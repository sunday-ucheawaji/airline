package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.UserException;
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
    public User getUserByEmail(String email) throws UserException {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UserException(String.format(ErrorMessageUtil.USER_NOT_FOUND_BY_EMAIL, email));
        }
        return user;
    }

    @Override
    public User getUserById(Long id) throws UserException {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserException(String.format(ErrorMessageUtil.USER_NOT_FOUND_BY_ID, id)));
    }

    @Override
    public List<User> getUsers() throws UserException {
        return userRepository.findAll();
    }
}
