package com.sunday.services.mapper;

import com.sunday.common_lib.dto.AuthUserDTO;
import com.sunday.services.model.User;

import java.util.List;
import java.util.stream.Collectors;

public class UserMapper {

    private UserMapper() {}

    public static AuthUserDTO toAuthUserDTO(User user) {
        AuthUserDTO dto = new AuthUserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setMiddleName(user.getMiddleName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setEmailVerified(user.getEmailVerified());
        dto.setLastLogin(user.getLastLogin());
        return dto;
    }

    public static List<AuthUserDTO> toAuthUserDTOList(List<User> users) {
        return users.stream()
                .map(UserMapper::toAuthUserDTO)
                .collect(Collectors.toList());
    }
}
