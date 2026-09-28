package com.sunday.services.controller;

import com.sunday.common_lib.payload.response.ReviewerResponse;
import com.sunday.services.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Service-to-service only: {@code /internal/**} is not routed by the api-gateway. */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final RoleService roleService;

    /** Active staff holding a platform role, e.g. {@code COMPLIANCE_OFFICER}; airline-core uses it to offer reviewers. */
    @GetMapping("/by-role/{roleName}")
    public List<ReviewerResponse> getReviewersByRole(@PathVariable String roleName) {
        return roleService.getReviewersByRole(roleName);
    }
}
