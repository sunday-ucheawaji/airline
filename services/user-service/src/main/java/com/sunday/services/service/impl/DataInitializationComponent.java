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

// Never runs outside "local" (must not seed a known-password account into a real environment);
// grants SUPER_ADMIN because the gateway restricts role assignment to that role, so without this
// seed nobody could ever be granted the first one through the API.
@Component
@Profile("local")
@RequiredArgsConstructor
public class DataInitializationComponent implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserPlatformRoleRepository userPlatformRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${bootstrap.admin.email}")
    private String adminEmail;

    @Value("${bootstrap.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        User admin = initializeAdminUser();
        grantPlatformAdminRole(admin);
    }

    private User initializeAdminUser() {
        User existing = userRepository.findByEmail(adminEmail);
        if (existing != null) {
            return existing;
        }

        User adminUser = new User();
        adminUser.setPassword(passwordEncoder.encode(adminPassword));
        adminUser.setFirstName("Sunday");
        adminUser.setLastName("Admin");
        adminUser.setEmail(adminEmail);
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
