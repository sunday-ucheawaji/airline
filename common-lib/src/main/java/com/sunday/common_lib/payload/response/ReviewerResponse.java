package com.sunday.common_lib.payload.response;

import lombok.*;

/** A staff member who can be picked for a case-owner or stage assignment. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewerResponse {

    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
}
