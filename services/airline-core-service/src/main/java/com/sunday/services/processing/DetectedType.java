package com.sunday.services.processing;

import java.util.Set;

/** The only file types onboarding documents may be; anything else is blocked. */
public enum DetectedType {
    PDF("application/pdf", Set.of("pdf")),
    PNG("image/png", Set.of("png")),
    JPEG("image/jpeg", Set.of("jpg", "jpeg"));

    private final String mimeType;
    private final Set<String> extensions;

    DetectedType(String mimeType, Set<String> extensions) {
        this.mimeType = mimeType;
        this.extensions = extensions;
    }

    public String mimeType() {
        return mimeType;
    }

    /** The file-name extensions that are consistent with this type. */
    public Set<String> extensions() {
        return extensions;
    }

    public boolean isImage() {
        return this != PDF;
    }
}
