package com.sunday.services.service.impl;

import com.sunday.services.repository.RoleRepository;
import com.sunday.services.enums.PermissionScope;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.constants.PlatformRoles;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.PermissionRequest;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.model.Permission;
import com.sunday.services.model.Role;
import com.sunday.services.model.RolePermission;
import com.sunday.services.repository.PermissionRepository;
import com.sunday.services.repository.RolePermissionRepository;
import com.sunday.services.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;


    @Override
    public List<Permission> getPermissions() {
        return permissionRepository.findAll();
    }

    /** A new PLATFORM permission is granted to SUPER_ADMIN straight away, which by definition holds every one. */
    @Override
    @Transactional
    public Permission createPermission(PermissionRequest request) {
        if (request.getName().startsWith(CustomUserDetailsService.ROLE_PREFIX)) {
            throw new BadRequestException(ErrorMessageUtil.PERMISSION_NAME_RESERVED_PREFIX);
        }
        if (permissionRepository.findByName(request.getName()) != null) {
            throw new ConflictException(String.format(ErrorMessageUtil.PERMISSION_ALREADY_EXISTS, request.getName()));
        }
        Permission permission = new Permission();
        permission.setName(request.getName());
        permission.setDescription(request.getDescription());
        permission.setScope(request.getScope() != null ? PermissionScope.valueOf(request.getScope()) : PermissionScope.PLATFORM);
        Permission saved = permissionRepository.save(permission);

        if (saved.getScope() == PermissionScope.PLATFORM) {
            Role superAdmin = roleRepository.findByName(PlatformRoles.SUPER_ADMIN);
            if (superAdmin != null) {
                RolePermission grant = new RolePermission();
                grant.setRole(superAdmin);
                grant.setPermission(saved);
                rolePermissionRepository.save(grant);
            }
        }
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getRolesForPermission(UUID permissionId) {
        if (!permissionRepository.existsById(permissionId)) {
            throw new ResourceNotFoundException(String.format(ErrorMessageUtil.PERMISSION_NOT_FOUND_BY_ID, permissionId));
        }

        return rolePermissionRepository.findByPermissionId(permissionId).stream()
                .map(RolePermission::getRole)
                .collect(Collectors.toList());
    }
}
