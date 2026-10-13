package com.sunday.services.model;

import com.sunday.services.enums.PermissionScope;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(name = "permissions")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Permission {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true)
    @NotBlank(message = ErrorMessageUtil.NAME_MANDATORY)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PermissionScope scope = PermissionScope.PLATFORM;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
