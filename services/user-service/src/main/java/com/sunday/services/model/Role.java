package com.sunday.services.model;

import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.enums.RoleScope;
import com.sunday.services.enums.RoleStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "roles")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true)
    @NotBlank(message = ErrorMessageUtil.NAME_MANDATORY)
    private String name;

    private String description;

    @Column(nullable = false)
    @NotNull(message = ErrorMessageUtil.STATUS_MANDATORY)
    @Enumerated(EnumType.STRING)
    private RoleStatus status = RoleStatus.ACTIVE;

    @Column(nullable = false)
    @NotNull(message = ErrorMessageUtil.SCOPE_MANDATORY)
    @Enumerated(EnumType.STRING)
    private RoleScope scope = RoleScope.AIRLINE;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
