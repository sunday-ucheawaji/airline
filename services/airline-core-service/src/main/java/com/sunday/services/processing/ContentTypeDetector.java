package com.sunday.services.processing;

import java.util.Locale;
import java.util.Optional;

/**
 * Decides what a file really is from its own first bytes, ignoring the name and the type the uploader claimed.
 * Deliberately strict: a PDF must start with its header at offset 0 (the specification tolerates junk before it,
 * which is a common polyglot trick), and only the three allowed formats are recognised at all.
 */
public final class ContentTypeDetector {

    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F', '-'};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private ContentTypeDetector() {
    }

    public static Optional<DetectedType> detect(byte[] content) {
        if (startsWith(content, PDF_MAGIC)) {
            return Optional.of(DetectedType.PDF);
        }
        if (startsWith(content, PNG_MAGIC)) {
            return Optional.of(DetectedType.PNG);
        }
        if (startsWith(content, JPEG_MAGIC)) {
            return Optional.of(DetectedType.JPEG);
        }
        return Optional.empty();
    }

    /**
     * True when the uploader claimed a specific type that differs from the real one. A missing or generic
     * ({@code application/octet-stream}) claim is not a contradiction: many clients send nothing better.
     */
    public static boolean declaredTypeContradicts(String declared, DetectedType detected) {
        if (declared == null || declared.isBlank()) {
            return false;
        }
        String claimed = normalise(declared.split(";")[0].strip().toLowerCase(Locale.ROOT));
        if (claimed.equals("application/octet-stream") || claimed.equals("binary/octet-stream")) {
            return false;
        }
        return !claimed.equals(detected.mimeType());
    }

    /** True when the file name has an extension that does not belong to the real type (e.g. {@code licence.exe} holding a PDF). */
    public static boolean extensionContradicts(String fileName, DetectedType detected) {
        if (fileName == null) {
            return false;
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return false;
        }
        return !detected.extensions().contains(fileName.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private static String normalise(String mime) {
        return switch (mime) {
            case "image/jpg", "image/pjpeg" -> "image/jpeg";
            case "application/x-pdf" -> "application/pdf";
            default -> mime;
        };
    }

    private static boolean startsWith(byte[] content, byte[] magic) {
        if (content == null || content.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (content[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }
}
