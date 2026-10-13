package com.sunday.common_lib.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AirlineRequest {

    @NotBlank
    private String name;

    private String alias;

    @NotBlank
    private String country;

    private String logoUrl;

    private String website;

    private String alliance;

    private UUID headquartersCityId;

    private String supportEmail;
    private String supportPhone;
    private String supportHours;
}
