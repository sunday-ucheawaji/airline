package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.AssignPermissionsRequest;
import com.sunday.common_lib.payload.request.RoleRequest;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.model.Permission;
import com.sunday.services.model.Role;
import com.sunday.services.model.RolePermission;
import com.sunday.services.repository.PermissionRepository;
import com.sunday.services.repository.RolePermissionRepository;
import com.sunday.services.repository.RoleRepository;
import com.sunday.services.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    public List<Role> getRoles() throws ResourceNotFoundException {
        return roleRepository.findAll();
    }

    @Override
    public Role getRoleById(Long id) throws ResourceNotFoundException {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.ROLE_NOT_FOUND_BY_ID, id)));
    }

    @Override
    public Role createRole(RoleRequest request) {
        if (roleRepository.findByName(request.getName()) != null) {
            throw new IllegalArgumentException(String.format(ErrorMessageUtil.ROLE_ALREADY_EXISTS, request.getName()));
        }

        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());

        return roleRepository.save(role);
    }

    @Override
    public List<Permission> getPermissionsForRole(Long roleId) throws ResourceNotFoundException {
        getRoleById(roleId);

        return rolePermissionRepository.findByRoleId(roleId).stream()
                .map(RolePermission::getPermission)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<Permission> assignPermissionsToRole(Long roleId, AssignPermissionsRequest request) throws ResourceNotFoundException {
        Role role = getRoleById(roleId);

        for (Long permissionId : request.getPermissionIds()) {
            Permission permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.PERMISSION_NOT_FOUND_BY_ID, permissionId)));

            if (!rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRole(role);
                rolePermission.setPermission(permission);
                rolePermissionRepository.save(rolePermission);
            }
        }

        return getPermissionsForRole(roleId);
    }

    @Override
    public void unassignPermissionFromRole(Long roleId, Long permissionId) throws ResourceNotFoundException {
        getRoleById(roleId);

        RolePermission rolePermission = rolePermissionRepository
                .findByRoleIdAndPermissionId(roleId, permissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.PERMISSION_NOT_ASSIGNED_TO_ROLE, permissionId, roleId)));

        rolePermissionRepository.delete(rolePermission);
    }
}
