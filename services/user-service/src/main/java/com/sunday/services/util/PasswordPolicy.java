package com.sunday.services.util;

import com.sunday.common_lib.exception.UserException;
import com.sunday.common_lib.util.ErrorMessageUtil;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {

    /** BCrypt only uses the first 72 bytes, and Spring Security refuses longer input outright. */
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {}

    /** Bean Validation counts characters; this counts the UTF-8 bytes BCrypt actually sees. */
    public static void requireHashable(String password) {
        if (password != null && password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new UserException(ErrorMessageUtil.PASSWORD_TOO_LONG);
        }
    }
}
