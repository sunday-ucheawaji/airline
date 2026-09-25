package com.sunday.services.service.impl;

import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.enums.RoleScope;
import com.sunday.services.enums.RoleStatus;
import com.sunday.services.model.Role;
import com.sunday.services.model.RolePermission;
import com.sunday.services.model.User;
import com.sunday.services.model.UserPlatformRole;
import com.sunday.services.repository.RolePermissionRepository;
import com.sunday.services.repository.RoleRepository;
import com.sunday.services.repository.UserPlatformRoleRepository;
import com.sunday.services.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;


@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserPlatformRoleRepository userPlatformRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public static final String ROLE_PREFIX = "ROLE_";

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new UsernameNotFoundException(String.format(ErrorMessageUtil.USER_NOT_FOUND_BY_EMAIL, email));
        }

        List<Role> roles = userPlatformRoleRepository.findByUserId(user.getId()).stream()
                .map(UserPlatformRole::getRole)
                .toList();
        if (roles.isEmpty()) {
            roles = roleRepository.findByScopeAndStatus(RoleScope.BASELINE, RoleStatus.ACTIVE);
        }

        List<String> roleNames = roles.stream().map(Role::getName).distinct().sorted().toList();
        List<String> permissionNames = roles.isEmpty() ? List.of()
                : rolePermissionRepository.findByRoleIn(roles).stream()
                        .map(RolePermission::getPermission)
                        .map(permission -> permission.getName())
                        .distinct()
                        .sorted()
                        .toList();

        List<GrantedAuthority> authorities = Stream.concat(
                        roleNames.stream().map(role -> ROLE_PREFIX + role),
                        permissionNames.stream())
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();

        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), authorities);
    }
}
