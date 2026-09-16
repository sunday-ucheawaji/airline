package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, message = ErrorMessageUtil.PASSWORD_MIN_LENGTH)
    private String password;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String middleName;

    private String phoneNumber;
}
