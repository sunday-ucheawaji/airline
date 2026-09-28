package com.sunday.services.enums;

import java.util.List;

/** What an uploaded onboarding document is. Two types are required before an application can be submitted. */
public enum DocumentType {
    CERTIFICATE_OF_INCORPORATION(true),
    AIR_OPERATOR_CERTIFICATE(true),
    OPERATING_LICENSE(false),
    BUSINESS_REGISTRATION(false),
    OTHER(false);

    private final boolean required;

    DocumentType(boolean required) {
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }

    public static List<DocumentType> requiredTypes() {
        return List.of(values()).stream().filter(DocumentType::isRequired).toList();
    }
}
