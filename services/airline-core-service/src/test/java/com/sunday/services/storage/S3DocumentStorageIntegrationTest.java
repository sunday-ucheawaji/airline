package com.sunday.services.storage;

import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.services.config.DocumentProperties;
import com.sunday.services.config.DocumentStorageConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Exercises the real S3 code against the dev object store from docker-compose.dev.yml (SeaweedFS). Skipped when the
 * store is not reachable, so the normal build never depends on Docker. Override with DOCUMENT_STORAGE_* variables.
 */
class S3DocumentStorageIntegrationTest {

    private static S3DocumentStorage storage;
    private static S3Client s3;

    @BeforeAll
    static void connect() {
        String endpoint = env("DOCUMENT_STORAGE_ENDPOINT", "http://localhost:8333");
        URI uri = URI.create(endpoint);
        assumeTrue(reachable(uri.getHost(), uri.getPort()), "object store not running at " + endpoint);

        DocumentProperties properties = new DocumentProperties();
        properties.getStorage().setEndpoint(endpoint);
        properties.getStorage().setAccessKey(env("DOCUMENT_STORAGE_ACCESS_KEY", "gdsdevkey"));
        properties.getStorage().setSecretKey(env("DOCUMENT_STORAGE_SECRET_KEY", "gdsdevsecret-change-me"));

        DocumentStorageConfig config = new DocumentStorageConfig();
        s3 = config.s3Client(properties);
        S3Presigner presigner = config.s3Presigner(properties);
        storage = new S3DocumentStorage(s3, presigner, properties);
        ensureBucket(storage.quarantineBucket());
        ensureBucket(storage.cleanBucket());
    }

    @Test
    void storesInQuarantinePromotesToCleanAndServesASignedDownload() throws Exception {
        String key = "it/" + UUID.randomUUID();
        byte[] content = "%PDF-1.7 pretend document".getBytes(StandardCharsets.UTF_8);

        storage.putQuarantine(key, new ByteArrayInputStream(content), content.length);
        assertThat(storage.openQuarantine(key).readAllBytes()).isEqualTo(content);

        storage.putClean(key, new ByteArrayInputStream(content), content.length, "application/pdf");
        storage.deleteQuarantine(key);
        assertThatThrownBy(() -> storage.openQuarantine(key).readAllBytes()).isInstanceOf(ServiceUnavailableException.class);

        DocumentStorage.PresignedDownload link = storage.presignDownload(key, "licence \"final\".pdf", "application/pdf");
        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(link.url()).GET().build(), HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(content);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("application/pdf");
        assertThat(response.headers().firstValue("Content-Disposition").orElse("")).startsWith("attachment").contains("licence__final_.pdf");
        assertThat(link.expiresAt()).isAfter(java.time.Instant.now());

        storage.deleteClean(key);
    }

    @Test
    void theCleanAreaIsNotReadableWithoutASignedLink() throws Exception {
        String key = "it/" + UUID.randomUUID();
        byte[] content = "secret".getBytes(StandardCharsets.UTF_8);
        storage.putClean(key, new ByteArrayInputStream(content), content.length, "application/pdf");

        String unsigned = env("DOCUMENT_STORAGE_ENDPOINT", "http://localhost:8333") + "/" + storage.cleanBucket() + "/" + key;
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(unsigned)).GET().build(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isIn(401, 403);
        storage.deleteClean(key);
    }

    private static void ensureBucket(String bucket) {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
    }

    private static boolean reachable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
