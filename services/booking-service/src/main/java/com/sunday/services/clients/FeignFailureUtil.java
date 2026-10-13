package com.sunday.services.clients;

import feign.FeignException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared helper for pulling the real downstream error message out of a Feign exception's JSON
 * error body, so a fallback can surface the actual rejection reason instead of a generic one.
 */
public final class FeignFailureUtil {

    private static final Pattern MESSAGE_FIELD = Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private FeignFailureUtil() {}

    public static String extractMessage(FeignException fe, String defaultMessage) {
        String body = fe.contentUTF8();
        if (body == null) {
            return defaultMessage;
        }
        Matcher matcher = MESSAGE_FIELD.matcher(body);
        return matcher.find() ? matcher.group(1).replace("\\\"", "\"") : defaultMessage;
    }
}
