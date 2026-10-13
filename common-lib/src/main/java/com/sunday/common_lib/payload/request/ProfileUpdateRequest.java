package com.sunday.common_lib.payload.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateRequest {

    /** Only non-null fields are applied; a blank first or last name is rejected. */
    @Size(min = 1, max = 100)
    private String firstName;

    @Size(min = 1, max = 100)
    private String lastName;

    @Size(max = 100)
    private String middleName;

    @Size(max = 30)
    private String phoneNumber;
}
