package com.sunday.common_lib.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
public class PermissionDTO {
    private UUID id;
    private String name;
    private String description;
}
