package com.sunday.common_lib.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
public class RoleDTO {
    private UUID id;
    private String name;
    private String description;
    private String status;
    private String scope;
}
