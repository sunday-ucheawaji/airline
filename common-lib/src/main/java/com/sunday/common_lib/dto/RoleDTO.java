package com.sunday.common_lib.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RoleDTO {
    private Long id;
    private String name;
    private String description;
    private String status;
}
