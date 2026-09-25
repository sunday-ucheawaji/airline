package com.sunday.common_lib.payload.request;

import lombok.*;

/** Optional reviewer note attached to a return / approve / reject / provision action. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingActionRequest {

    private String comments;
}
