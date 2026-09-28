package com.sunday.services.storage;

import com.sunday.common_lib.exception.ServiceUnavailableException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Test double for {@link DocumentStorage}: two in-memory areas and a switch to simulate an outage. */
public class InMemoryDocumentStorage implements DocumentStorage {

    public final Map<String, byte[]> quarantine = new HashMap<>();
    public final Map<String, byte[]> clean = new HashMap<>();
    public final Map<String, String> cleanContentTypes = new HashMap<>();
    public boolean unavailable;

    @Override
    public String provider() {
        return "MEMORY";
    }

    @Override
    public String quarantineBucket() {
        return "quarantine";
    }

    @Override
    public String cleanBucket() {
        return "clean";
    }

    @Override
    public void putQuarantine(String key, InputStream data, long size) {
        ensureAvailable();
        quarantine.put(key, read(data));
    }

    @Override
    public InputStream openQuarantine(String key) {
        ensureAvailable();
        byte[] bytes = quarantine.get(key);
        if (bytes == null) {
            throw new ServiceUnavailableException("missing object " + key);
        }
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public void deleteQuarantine(String key) {
        ensureAvailable();
        quarantine.remove(key);
    }

    @Override
    public void putClean(String key, InputStream data, long size, String contentType) {
        ensureAvailable();
        clean.put(key, read(data));
        cleanContentTypes.put(key, contentType);
    }

    @Override
    public void deleteClean(String key) {
        ensureAvailable();
        clean.remove(key);
        cleanContentTypes.remove(key);
    }

    @Override
    public PresignedDownload presignDownload(String cleanKey, String fileName, String contentType) {
        ensureAvailable();
        return new PresignedDownload(URI.create("http://storage.test/" + cleanKey), Instant.now().plusSeconds(300));
    }

    private void ensureAvailable() {
        if (unavailable) {
            throw new ServiceUnavailableException("storage down");
        }
    }

    private static byte[] read(InputStream data) {
        try (data) {
            return data.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
