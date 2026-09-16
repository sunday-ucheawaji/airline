package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.OperationNotPermittedException;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    public List<Permission> getPermissions() {
        return permissionRepository.findAll();
    }

    @Override
    public Permission createPermission(PermissionRequest request) {
        if (permissionRepository.findByName(request.getName()) != null) {
            throw new OperationNotPermittedException(String.format(ErrorMessageUtil.PERMISSION_ALREADY_EXISTS, request.getName()));
        }

        Permission permission = new Permission();
        permission.setName(request.getName());
        permission.setDescription(request.getDescription());

        return permissionRepository.save(permission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getRolesForPermission(Long permissionId) {
        if (!permissionRepository.existsById(permissionId)) {
            throw new ResourceNotFoundException(String.format(ErrorMessageUtil.PERMISSION_NOT_FOUND_BY_ID, permissionId));
        }

        return rolePermissionRepository.findByPermissionId(permissionId).stream()
                .map(RolePermission::getRole)
                .collect(Collectors.toList());
    }
}
