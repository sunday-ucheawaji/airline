package com.sunday.services.enums;

import com.sunday.common_lib.constants.PlatformRoles;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.util.ErrorMessageUtil;

/** The specialist review stages every application goes through before final approval. */
public enum OnboardingStage {
    COMPLIANCE(PlatformRoles.COMPLIANCE_OFFICER),
    COMMERCIAL(PlatformRoles.COMMERCIAL_OFFICER),
    TECHNICAL(PlatformRoles.TECHNICAL_OFFICER);

    private final String requiredRole;

    OnboardingStage(String requiredRole) {
        this.requiredRole = requiredRole;
    }

    /** The platform role a user must hold to be assigned this stage. */
    public String requiredRole() {
        return requiredRole;
    }

    /** Parses the lower-case stage segment used in URLs, e.g. {@code compliance}. */
    public static OnboardingStage fromPath(String value) {
        for (OnboardingStage stage : values()) {
            if (stage.name().equalsIgnoreCase(value)) {
                return stage;
            }
        }
        throw new BadRequestException(String.format(ErrorMessageUtil.ONBOARDING_STAGE_UNKNOWN, value));
    }
}
