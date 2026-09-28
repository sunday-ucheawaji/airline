package com.sunday.services.storage;

import java.io.InputStream;
import java.net.URI;
import java.time.Instant;

/**
 * Private object storage for onboarding documents, split in two areas: {@code quarantine} holds raw uploads nobody may
 * read, {@code clean} holds files that passed the automatic checks. Failures surface as
 * {@link com.sunday.common_lib.exception.ServiceUnavailableException}.
 */
public interface DocumentStorage {

    /** Where the metadata records the object lives, e.g. {@code S3}. */
    String provider();

    String quarantineBucket();

    String cleanBucket();

    void putQuarantine(String key, InputStream data, long size);

    InputStream openQuarantine(String key);

    void deleteQuarantine(String key);

    void putClean(String key, InputStream data, long size, String contentType);

    void deleteClean(String key);

    /** A short-lived signed link that forces a download with the given content type; the file stays private. */
    PresignedDownload presignDownload(String cleanKey, String fileName, String contentType);

    record PresignedDownload(URI url, Instant expiresAt) {
    }
}
