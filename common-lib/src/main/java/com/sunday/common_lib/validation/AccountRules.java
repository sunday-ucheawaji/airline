package com.sunday.common_lib.validation;

/** Limits shared by the account request DTOs; each matches its column in user-service's {@code users} table. */
public final class AccountRules {

    public static final int EMAIL_MAX = 254;
    public static final int NAME_MAX = 100;
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX_BYTES = 72;

    /** A letter first, then letters, combining marks, spaces, apostrophes, hyphens or full stops. Blank is left to {@code @NotBlank}. */
    public static final String NAME_PATTERN = "^\\s*(\\p{L}[\\p{L}\\p{M} '.\\-]*)?$";

    /** E.164: a plus, a country code, up to 15 digits. Blank means no number. */
    public static final String PHONE_PATTERN = "^$|^\\+[1-9]\\d{6,14}$";

    private AccountRules() {}
}
