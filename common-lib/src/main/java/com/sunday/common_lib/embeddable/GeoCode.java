package com.sunday.common_lib.embeddable;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeoCode {

    @DecimalMin(value = "-90.0", message = ErrorMessageUtil.AIRPORT_LATITUDE_OUT_OF_RANGE)
    @DecimalMax(value = "90.0", message = ErrorMessageUtil.AIRPORT_LATITUDE_OUT_OF_RANGE)
    private Double latitude;

    @DecimalMin(value = "-180.0", message = ErrorMessageUtil.AIRPORT_LONGITUDE_OUT_OF_RANGE)
    @DecimalMax(value = "180.0", message = ErrorMessageUtil.AIRPORT_LONGITUDE_OUT_OF_RANGE)
    private Double longitude;
}
