package com.sunday.services.service.impl;

import com.sunday.common_lib.payload.response.ReviewerResponse;
import com.sunday.services.enums.UserStatus;
import com.sunday.services.model.User;
import com.sunday.services.model.UserPlatformRole;
import com.sunday.services.repository.UserPlatformRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplReviewersTest {

    @Mock UserPlatformRoleRepository userPlatformRoleRepository;
    @InjectMocks RoleServiceImpl service;

    @Test
    void returnsIdNameAndEmailOfActiveHoldersOnly() {
        when(userPlatformRoleRepository.findByRoleName("COMPLIANCE_OFFICER")).thenReturn(List.of(
                grant(user(4L, "Ada", "Obi", "ada@x.com", UserStatus.ACTIVE)),
                grant(user(5L, "Bo", "Lee", "bo@x.com", UserStatus.SUSPENDED)),
                grant(user(6L, "Cy", "Ng", "cy@x.com", UserStatus.LOCKED))));

        List<ReviewerResponse> reviewers = service.getReviewersByRole("COMPLIANCE_OFFICER");

        assertThat(reviewers).hasSize(1);
        assertThat(reviewers.get(0).getUserId()).isEqualTo(4L);
        assertThat(reviewers.get(0).getFirstName()).isEqualTo("Ada");
        assertThat(reviewers.get(0).getLastName()).isEqualTo("Obi");
        assertThat(reviewers.get(0).getEmail()).isEqualTo("ada@x.com");
    }

    @Test
    void noHoldersGivesAnEmptyList() {
        when(userPlatformRoleRepository.findByRoleName("TECHNICAL_OFFICER")).thenReturn(List.of());

        assertThat(service.getReviewersByRole("TECHNICAL_OFFICER")).isEmpty();
    }

    private static User user(Long id, String first, String last, String email, UserStatus status) {
        User u = new User();
        u.setId(id);
        u.setFirstName(first);
        u.setLastName(last);
        u.setEmail(email);
        u.setStatus(status);
        return u;
    }

    private static UserPlatformRole grant(User user) {
        UserPlatformRole g = new UserPlatformRole();
        g.setUser(user);
        return g;
    }
}
