package com.sunday.services.service.impl;

import com.sunday.services.enums.RoleScope;
import com.sunday.services.enums.RoleStatus;
import com.sunday.services.model.Permission;
import com.sunday.services.model.Role;
import com.sunday.services.model.RolePermission;
import com.sunday.services.model.User;
import com.sunday.services.model.UserPlatformRole;
import com.sunday.services.repository.RolePermissionRepository;
import com.sunday.services.repository.RoleRepository;
import com.sunday.services.repository.UserPlatformRoleRepository;
import com.sunday.services.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserPlatformRoleRepository userPlatformRoleRepository;
    @Mock RoleRepository roleRepository;
    @Mock RolePermissionRepository rolePermissionRepository;
    @InjectMocks CustomUserDetailsService service;

    private final User user = user();

    @Test
    void staffGetTheirPlatformRolesAndThoseRolesPermissionsOnly() {
        Role approver = role("SENIOR_APPROVER", RoleScope.PLATFORM);
        when(userRepository.findByEmail("a@x.com")).thenReturn(user);
        when(userPlatformRoleRepository.findByUserId(7L)).thenReturn(List.of(grant(approver)));
        when(rolePermissionRepository.findByRoleIn(List.of(approver))).thenReturn(List.of(
                link(approver, "ONBOARDING_FINAL_APPROVE"), link(approver, "ONBOARDING_APPLICATION_READ")));

        UserDetails details = service.loadUserByUsername("a@x.com");

        assertThat(authorities(details))
                .containsExactlyInAnyOrder("ROLE_SENIOR_APPROVER", "ONBOARDING_APPLICATION_READ", "ONBOARDING_FINAL_APPROVE");
        assertThat(details.getUsername()).isEqualTo("a@x.com");
        assertThat(details.getPassword()).isEqualTo("hash");
        verify(roleRepository, never()).findByScopeAndStatus(any(), any());
    }

    @Test
    void everyoneElseGetsTheBaselineRoleAndItsPermissions() {
        Role applicant = role("AIRLINE_APPLICANT", RoleScope.BASELINE);
        when(userRepository.findByEmail("a@x.com")).thenReturn(user);
        when(userPlatformRoleRepository.findByUserId(7L)).thenReturn(List.of());
        when(roleRepository.findByScopeAndStatus(RoleScope.BASELINE, RoleStatus.ACTIVE)).thenReturn(List.of(applicant));
        when(rolePermissionRepository.findByRoleIn(List.of(applicant))).thenReturn(List.of(
                link(applicant, "ONBOARDING_APPLICATION_SUBMIT"), link(applicant, "ONBOARDING_APPLICATION_CREATE")));

        UserDetails details = service.loadUserByUsername("a@x.com");

        assertThat(authorities(details))
                .containsExactlyInAnyOrder("ROLE_AIRLINE_APPLICANT", "ONBOARDING_APPLICATION_CREATE", "ONBOARDING_APPLICATION_SUBMIT");
    }

    @Test
    void permissionsSharedByTwoRolesAppearOnce() {
        Role officer = role("ONBOARDING_OFFICER", RoleScope.PLATFORM);
        Role compliance = role("COMPLIANCE_OFFICER", RoleScope.PLATFORM);
        when(userRepository.findByEmail("a@x.com")).thenReturn(user);
        when(userPlatformRoleRepository.findByUserId(7L)).thenReturn(List.of(grant(officer), grant(compliance)));
        when(rolePermissionRepository.findByRoleIn(List.of(officer, compliance))).thenReturn(List.of(
                link(officer, "ONBOARDING_APPLICATION_READ"), link(compliance, "ONBOARDING_APPLICATION_READ")));

        UserDetails details = service.loadUserByUsername("a@x.com");

        assertThat(authorities(details))
                .containsExactlyInAnyOrder("ROLE_COMPLIANCE_OFFICER", "ROLE_ONBOARDING_OFFICER", "ONBOARDING_APPLICATION_READ");
    }

    @Test
    void aUserWithNoRolesAtAllStillLoadsWithoutLookingUpPermissions() {
        when(userRepository.findByEmail("a@x.com")).thenReturn(user);
        when(userPlatformRoleRepository.findByUserId(7L)).thenReturn(List.of());
        when(roleRepository.findByScopeAndStatus(RoleScope.BASELINE, RoleStatus.ACTIVE)).thenReturn(List.of());

        UserDetails details = service.loadUserByUsername("a@x.com");

        assertThat(details.getAuthorities()).isEmpty();
        verify(rolePermissionRepository, never()).findByRoleIn(any());
    }

    @Test
    void anUnknownEmailIsRejected() {
        when(userRepository.findByEmail("nobody@x.com")).thenReturn(null);

        assertThatThrownBy(() -> service.loadUserByUsername("nobody@x.com")).isInstanceOf(UsernameNotFoundException.class);
    }

    private static List<String> authorities(UserDetails details) {
        return details.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    private static User user() {
        User u = new User();
        u.setId(7L);
        u.setEmail("a@x.com");
        u.setPassword("hash");
        return u;
    }

    private static Role role(String name, RoleScope scope) {
        Role r = new Role();
        r.setName(name);
        r.setScope(scope);
        return r;
    }

    private UserPlatformRole grant(Role role) {
        UserPlatformRole g = new UserPlatformRole();
        g.setUser(user);
        g.setRole(role);
        return g;
    }

    private static RolePermission link(Role role, String permissionName) {
        Permission p = new Permission();
        p.setName(permissionName);
        RolePermission rp = new RolePermission();
        rp.setRole(role);
        rp.setPermission(p);
        return rp;
    }
}
