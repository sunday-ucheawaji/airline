package com.sunday.services.service.impl;

import com.sunday.common_lib.enums.UserRole;
import com.sunday.services.model.User;
import com.sunday.services.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializationComponent implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;



    @Override
    public void run(String... args) {
        initializeAdminUser();
    }

    private void initializeAdminUser() {
        String adminUsername = "codewithzosh@gmail.com";

        if (userRepository.findByEmail(adminUsername)==null) {
            User adminUser = new User();

            adminUser.setPassword(passwordEncoder.encode("codewithzosh"));
            adminUser.setFullName("zosh");
            adminUser.setEmail(adminUsername);
            adminUser.setRole(UserRole.ROLE_SYSTEM_ADMIN);

            User admin=userRepository.save(adminUser);
        }
    }
}
