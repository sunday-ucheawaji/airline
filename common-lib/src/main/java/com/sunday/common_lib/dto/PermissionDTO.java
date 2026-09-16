package com.sunday.common_lib.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PermissionDTO {
    private Long id;
    private String name;
    private String description;
}
