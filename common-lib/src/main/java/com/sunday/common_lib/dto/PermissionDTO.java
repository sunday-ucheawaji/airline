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
    /** PLATFORM (carried in the JWT) or AIRLINE (checked per airline membership in airline-core-service). */
    private String scope;
}
