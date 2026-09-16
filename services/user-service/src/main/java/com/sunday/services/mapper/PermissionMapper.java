package com.sunday.services.mapper;

import com.sunday.common_lib.dto.PermissionDTO;
import com.sunday.services.model.Permission;

import java.util.List;
import java.util.stream.Collectors;

public class PermissionMapper {

    private PermissionMapper() {}

    public static PermissionDTO toDTO(Permission permission) {
        PermissionDTO dto = new PermissionDTO();
        dto.setId(permission.getId());
        dto.setName(permission.getName());
        dto.setDescription(permission.getDescription());
        return dto;
    }

    public static List<PermissionDTO> toDTOList(List<Permission> permissions) {
        return permissions.stream()
                .map(PermissionMapper::toDTO)
                .collect(Collectors.toList());
    }
}
