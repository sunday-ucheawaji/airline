package com.sunday.common_lib.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class AuthUserDTO {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String middleName;
    private String phoneNumber;
    private Boolean emailVerified;
    private String status;
    private LocalDateTime lastLogin;
}
