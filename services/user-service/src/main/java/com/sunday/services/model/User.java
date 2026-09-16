package com.sunday.services.model;


import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.enums.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = ErrorMessageUtil.FIRST_NAME_MANDATORY)
    private String firstName;

    @NotBlank(message = ErrorMessageUtil.LAST_NAME_MANDATORY)
    private String lastName;

    private String middleName;

    private String password;

    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true)
    @NotBlank(message = ErrorMessageUtil.EMAIL_MANDATORY)
    @Email(message = ErrorMessageUtil.EMAIL_INVALID)
    private String email;

    private String phoneNumber;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    @Column(nullable = false)
    @NotNull(message = ErrorMessageUtil.STATUS_MANDATORY)
    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false)
    private Integer failedLoginAttempts = 0;

    private LocalDateTime lockedUntil;

    private LocalDateTime lastLogin;


//    @JsonIgnore
//    private List<Long> passengers = new ArrayList<>();


}


