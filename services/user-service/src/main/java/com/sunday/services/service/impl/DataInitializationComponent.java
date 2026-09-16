package com.sunday.services.service.impl;

import com.sunday.services.model.User;
import com.sunday.services.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a local/dev-only admin account so there's something to log in with
 * without a manual DB insert. Never runs outside the "local" profile —
 * this must not seed a known-password account into a real environment.
 */
@Component
@Profile("local")
@RequiredArgsConstructor
public class DataInitializationComponent implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "codewithzosh@gmail.com";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${bootstrap.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        initializeAdminUser();
    }

    private void initializeAdminUser() {
        if (userRepository.findByEmail(ADMIN_EMAIL) != null) {
            return;
        }

        User adminUser = new User();
        adminUser.setPassword(passwordEncoder.encode(adminPassword));
        adminUser.setFirstName("Zosh");
        adminUser.setLastName("Admin");
        adminUser.setEmail(ADMIN_EMAIL);
        adminUser.setEmailVerified(true);

        userRepository.save(adminUser);
    }
}
