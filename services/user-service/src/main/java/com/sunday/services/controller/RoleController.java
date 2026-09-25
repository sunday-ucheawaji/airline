package com.sunday.services.controller;

import com.sunday.common_lib.dto.PermissionDTO;
import com.sunday.common_lib.dto.RoleDTO;
import com.sunday.common_lib.payload.request.AssignPermissionsRequest;
import com.sunday.common_lib.payload.request.RoleRequest;
import com.sunday.services.mapper.PermissionMapper;
import com.sunday.services.mapper.RoleMapper;
import com.sunday.services.model.Permission;
import com.sunday.services.model.Role;
import com.sunday.services.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<RoleDTO> createRole(
            @Valid @RequestBody RoleRequest request) {
        Role role = roleService.createRole(request);
        RoleDTO dto = RoleMapper.toDTO(role);
        return ResponseEntity.created(URI.create("/api/roles/" + dto.getId())).body(dto);
    }

    @GetMapping
    public ResponseEntity<List<RoleDTO>> getRoles() {
        List<Role> roles = roleService.getRoles();
        return ResponseEntity.ok(RoleMapper.toDTOList(roles));
    }

    @GetMapping("/{roleId}")
    public ResponseEntity<RoleDTO> getRoleById(
            @PathVariable Long roleId) {
        Role role = roleService.getRoleById(roleId);
        return ResponseEntity.ok(RoleMapper.toDTO(role));
    }

    @GetMapping("/{roleId}/permissions")
    public ResponseEntity<List<PermissionDTO>> getRolePermissions(
            @PathVariable Long roleId) {
        List<Permission> permissions = roleService.getPermissionsForRole(roleId);
        return ResponseEntity.ok(PermissionMapper.toDTOList(permissions));
    }

    @PostMapping("/{roleId}/permissions")
    public ResponseEntity<List<PermissionDTO>> assignPermissionsToRole(
            @PathVariable Long roleId,
            @Valid @RequestBody AssignPermissionsRequest request) {
        List<Permission> permissions = roleService.assignPermissionsToRole(roleId, request);
        return ResponseEntity.ok(PermissionMapper.toDTOList(permissions));
    }

    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    public ResponseEntity<Void> unassignPermissionFromRole(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {
        roleService.unassignPermissionFromRole(roleId, permissionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{roleId}/users/{userId}")
    public ResponseEntity<Void> assignPlatformRoleToUser(
            @PathVariable Long roleId,
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long grantorUserId,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String grantorRoles) {
        roleService.assignPlatformRoleToUser(roleId, userId, grantorUserId, parseRoles(grantorRoles));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{roleId}/users/{userId}")
    public ResponseEntity<Void> unassignPlatformRoleFromUser(
            @PathVariable Long roleId,
            @PathVariable Long userId,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String grantorRoles) {
        roleService.unassignPlatformRoleFromUser(roleId, userId, parseRoles(grantorRoles));
        return ResponseEntity.noContent().build();
    }

    private static List<String> parseRoles(String header) {
        return Arrays.stream(header.split(",")).map(String::trim).filter(r -> !r.isEmpty()).toList();
    }
}
