package com.sunday.services.storage;

import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.config.DocumentProperties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.function.Supplier;

/** {@link DocumentStorage} on any S3-compatible server (SeaweedFS in dev, Cloudflare R2 or AWS S3 in production). */
@Component
public class S3DocumentStorage implements DocumentStorage {

    private static final String PROVIDER = "S3";
    private static final int MAX_DOWNLOAD_NAME_LENGTH = 100;

    private final S3Client s3;
    private final S3Presigner presigner;
    private final DocumentProperties.Storage settings;

    public S3DocumentStorage(S3Client s3, S3Presigner presigner, DocumentProperties properties) {
        this.s3 = s3;
        this.presigner = presigner;
        this.settings = properties.getStorage();
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public String quarantineBucket() {
        return settings.getQuarantineBucket();
    }

    @Override
    public String cleanBucket() {
        return settings.getCleanBucket();
    }

    @Override
    public void putQuarantine(String key, InputStream data, long size) {
        // Stored as opaque bytes: the content type is decided later, from the file itself.
        put(quarantineBucket(), key, data, size, "application/octet-stream");
    }

    @Override
    public InputStream openQuarantine(String key) {
        return guard(() -> s3.getObject(GetObjectRequest.builder().bucket(quarantineBucket()).key(key).build()));
    }

    @Override
    public void deleteQuarantine(String key) {
        delete(quarantineBucket(), key);
    }

    @Override
    public void putClean(String key, InputStream data, long size, String contentType) {
        put(cleanBucket(), key, data, size, contentType);
    }

    @Override
    public void deleteClean(String key) {
        delete(cleanBucket(), key);
    }

    @Override
    public PresignedDownload presignDownload(String cleanKey, String fileName, String contentType) {
        Duration ttl = settings.getPresignTtl();
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(cleanBucket())
                .key(cleanKey)
                // Forced download with the type we detected, so a browser never renders or sniffs the file.
                .responseContentType(contentType)
                .responseContentDisposition("attachment; filename=\"" + safeDownloadName(fileName) + "\"")
                .responseCacheControl("no-store")
                .build();
        PresignedGetObjectRequest presigned = guard(() -> presigner.presignGetObject(
                GetObjectPresignRequest.builder().signatureDuration(ttl).getObjectRequest(request).build()));
        return new PresignedDownload(URI.create(presigned.url().toString()), presigned.expiration());
    }

    private void put(String bucket, String key, InputStream data, long size, String contentType) {
        guard(() -> s3.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).contentLength(size).build(),
                RequestBody.fromInputStream(data, size)));
    }

    private void delete(String bucket, String key) {
        guard(() -> s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build()));
    }

    private static <T> T guard(Supplier<T> call) {
        try {
            return call.get();
        } catch (RuntimeException e) {
            throw new ServiceUnavailableException(ErrorMessageUtil.DOCUMENT_STORAGE_UNAVAILABLE, e);
        }
    }

    /** Keeps only characters that are safe inside a quoted header value. */
    static String safeDownloadName(String fileName) {
        String cleaned = fileName == null ? "" : fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (cleaned.length() > MAX_DOWNLOAD_NAME_LENGTH) {
            cleaned = cleaned.substring(cleaned.length() - MAX_DOWNLOAD_NAME_LENGTH);
        }
        return cleaned.isBlank() || cleaned.chars().allMatch(c -> c == '.' || c == '_') ? "document" : cleaned;
    }
}
