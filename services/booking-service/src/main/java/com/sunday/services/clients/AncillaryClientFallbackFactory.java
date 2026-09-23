package com.sunday.services.clients;

import com.sunday.common_lib.payload.response.FlightCabinAncillaryResponse;
import com.sunday.common_lib.payload.response.FlightMealResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The price calls must never fall back to 0 — that would let unavailable, unknown or
 * unpriced extras through for free. A 4xx from ancillary-service is a rejection of the
 * customer's selection and is passed on with its message; anything else is a 503.
 * The read calls only feed booking display, so they still degrade to empty lists.
 */
@Component
@Slf4j
public class AncillaryClientFallbackFactory implements FallbackFactory<AncillaryClient> {

    private static final Pattern MESSAGE_FIELD = Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    @Override
    public AncillaryClient create(Throwable cause) {
        return new AncillaryClient() {

            @Override
            public double calculateAncillariesPrice(List<Long> flightCabinAncillaryIds) {
                throw priceFailure("ancillaries", cause);
            }

            @Override
            public List<FlightCabinAncillaryResponse> getAllByIds(List<Long> Ids) {
                return Collections.emptyList();
            }

            @Override
            public List<FlightMealResponse> getMealsByIds(List<Long> Ids) {
                return Collections.emptyList();
            }

            @Override
            public Double calculateMealPrice(List<Long> requests) {
                throw priceFailure("meals", cause);
            }
        };
    }

    private ResponseStatusException priceFailure(String what, Throwable cause) {
        if (cause instanceof FeignException fe && fe.status() >= 400 && fe.status() < 500) {
            log.warn("ancillary-service rejected {} selection: {}", what, fe.getMessage());
            return new ResponseStatusException(
                    HttpStatus.valueOf(fe.status()), extractMessage(fe, "Invalid " + what + " selection"));
        }
        log.error("Could not price {} via ancillary-service", what, cause);
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "Pricing for " + what + " is unavailable. Please retry.");
    }

    private String extractMessage(FeignException fe, String defaultMessage) {
        String body = fe.contentUTF8();
        if (body == null) {
            return defaultMessage;
        }
        Matcher matcher = MESSAGE_FIELD.matcher(body);
        return matcher.find() ? matcher.group(1).replace("\\\"", "\"") : defaultMessage;
    }
}
