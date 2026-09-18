package com.sunday.services.mapper;

import com.sunday.common_lib.dto.RoleDTO;
import com.sunday.services.model.Role;

import java.util.List;
import java.util.stream.Collectors;

public class RoleMapper {

    private RoleMapper() {}

    public static RoleDTO toDTO(Role role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setStatus(role.getStatus() != null ? role.getStatus().name() : null);
        dto.setScope(role.getScope() != null ? role.getScope().name() : null);
        return dto;
    }

    public static List<RoleDTO> toDTOList(List<Role> roles) {
        return roles.stream()
                .map(RoleMapper::toDTO)
                .collect(Collectors.toList());
    }
}
