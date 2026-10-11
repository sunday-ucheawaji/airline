package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CityRequest {

    @NotBlank(message = ErrorMessageUtil.CITY_NAME_MANDATORY)
    @Size(max = 100)
    private String name;

    @NotBlank(message = ErrorMessageUtil.CITY_CODE_MANDATORY)
    @Size(max = 10)
    private String cityCode;

    @NotBlank(message = ErrorMessageUtil.COUNTRY_CODE_MANDATORY)
    @Size(max = 5)
    private String countryCode;

    @NotBlank(message = ErrorMessageUtil.COUNTRY_NAME_MANDATORY)
    @Size(max = 100)
    private String countryName;

    @Size(max = 10)
    private String regionCode;

    @Size(max = 50)
    private String timeZoneId;
}
