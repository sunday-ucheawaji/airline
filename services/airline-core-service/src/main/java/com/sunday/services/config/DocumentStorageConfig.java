package com.sunday.services.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/** S3 client and presigner for the document store; works against any S3-compatible server. */
@Configuration
@EnableConfigurationProperties(DocumentProperties.class)
public class DocumentStorageConfig {

    @Bean
    public S3Client s3Client(DocumentProperties properties) {
        DocumentProperties.Storage storage = properties.getStorage();
        var builder = S3Client.builder()
                .region(Region.of(storage.getRegion()))
                .credentialsProvider(credentials(storage))
                .forcePathStyle(storage.isPathStyleAccess())
                // Newer SDK versions add checksums by default that several S3-compatible servers reject.
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);
        if (StringUtils.hasText(storage.getEndpoint())) {
            builder.endpointOverride(URI.create(storage.getEndpoint()));
        }
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner(DocumentProperties properties) {
        DocumentProperties.Storage storage = properties.getStorage();
        var builder = S3Presigner.builder()
                .region(Region.of(storage.getRegion()))
                .credentialsProvider(credentials(storage))
                // With a custom configuration, checksum validation must be off so the signed link stays usable
                // from a browser (a signed checksum header could not be sent).
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(storage.isPathStyleAccess())
                        .checksumValidationEnabled(false)
                        .build());
        if (StringUtils.hasText(storage.getEndpoint())) {
            builder.endpointOverride(URI.create(storage.getEndpoint()));
        }
        return builder.build();
    }

    private static StaticCredentialsProvider credentials(DocumentProperties.Storage storage) {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(storage.getAccessKey(), storage.getSecretKey()));
    }
}
