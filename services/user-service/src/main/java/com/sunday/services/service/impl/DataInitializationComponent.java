package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.PlatformRoles;
import com.sunday.services.model.Role;
import com.sunday.services.model.User;
import com.sunday.services.model.UserPlatformRole;
import com.sunday.services.repository.RoleRepository;
import com.sunday.services.repository.UserPlatformRoleRepository;
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
 *
 * The admin is also granted the PLATFORM-scoped SUPER_ADMIN role (seeded by Flyway): the api-gateway
 * restricts role assignment (and every other admin endpoint) to that role, so
 * without this seed nobody could ever be granted the first one through the API.
 */
@Component
@Profile("local")
@RequiredArgsConstructor
public class DataInitializationComponent implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "codewithzosh@gmail.com";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserPlatformRoleRepository userPlatformRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${bootstrap.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        User admin = initializeAdminUser();
        grantPlatformAdminRole(admin);
    }

    private User initializeAdminUser() {
        User existing = userRepository.findByEmail(ADMIN_EMAIL);
        if (existing != null) {
            return existing;
        }

        User adminUser = new User();
        adminUser.setPassword(passwordEncoder.encode(adminPassword));
        adminUser.setFirstName("Zosh");
        adminUser.setLastName("Admin");
        adminUser.setEmail(ADMIN_EMAIL);
        adminUser.setEmailVerified(true);

        return userRepository.save(adminUser);
    }

    private void grantPlatformAdminRole(User admin) {
        Role role = roleRepository.findByName(PlatformRoles.SUPER_ADMIN);
        if (role == null) {
            throw new IllegalStateException("Role " + PlatformRoles.SUPER_ADMIN + " is missing; the V8 access-model seed did not run");
        }

        if (!userPlatformRoleRepository.existsByUserIdAndRoleId(admin.getId(), role.getId())) {
            UserPlatformRole grant = new UserPlatformRole();
            grant.setUser(admin);
            grant.setRole(role);
            userPlatformRoleRepository.save(grant);
        }
    }
}
