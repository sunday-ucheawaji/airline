package com.sunday.services.controller;

import com.sunday.common_lib.dto.PermissionDTO;
import com.sunday.common_lib.dto.RoleDTO;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.PermissionRequest;
import com.sunday.services.mapper.PermissionMapper;
import com.sunday.services.mapper.RoleMapper;
import com.sunday.services.model.Permission;
import com.sunday.services.model.Role;
import com.sunday.services.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping
    public ResponseEntity<PermissionDTO> createPermission(
            @Valid @RequestBody PermissionRequest request) {
        Permission permission = permissionService.createPermission(request);
        return ResponseEntity.ok(PermissionMapper.toDTO(permission));
    }

    @GetMapping
    public ResponseEntity<List<PermissionDTO>> getPermissions() {
        List<Permission> permissions = permissionService.getPermissions();
        return ResponseEntity.ok(PermissionMapper.toDTOList(permissions));
    }

    @GetMapping("/{permissionId}/roles")
    public ResponseEntity<List<RoleDTO>> getRolesForPermission(
            @PathVariable Long permissionId) throws ResourceNotFoundException {
        List<Role> roles = permissionService.getRolesForPermission(permissionId);
        return ResponseEntity.ok(RoleMapper.toDTOList(roles));
    }
}
