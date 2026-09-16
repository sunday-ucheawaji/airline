package com.sunday.common_lib.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sunday.common_lib.dto.AuthUserDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String jwt;
    private String refreshToken;
    private String message;
    private String title;
    private AuthUserDTO user;
}
