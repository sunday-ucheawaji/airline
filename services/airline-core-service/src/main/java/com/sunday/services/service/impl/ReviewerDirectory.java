package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.payload.response.ReviewerResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.client.UserClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Looks up the staff who can be picked for a case or a stage; fails closed when user-service cannot answer. */
@Component
@RequiredArgsConstructor
public class ReviewerDirectory {

    private final UserClient userClient;

    public List<ReviewerResponse> byRole(String roleName) {
        try {
            return userClient.getReviewersByRole(roleName);
        } catch (Exception e) {
            throw new ServiceUnavailableException(ErrorMessageUtil.USER_ROLE_CHECK_UNAVAILABLE, e);
        }
    }
}
