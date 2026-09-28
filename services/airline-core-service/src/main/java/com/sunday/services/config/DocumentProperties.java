package com.sunday.services.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Limits and storage settings for onboarding documents ({@code airline.documents.*}). */
@Getter
@Setter
@ConfigurationProperties(prefix = "airline.documents")
public class DocumentProperties {

    private long maxFileSizeBytes = 10L * 1024 * 1024;
    private int maxDocumentsPerApplication = 20;
    private int maxPdfPages = 50;
    private long maxImagePixels = 25_000_000L;

    /** A document still PROCESSING after this long is assumed to have lost its event and is republished. */
    private Duration staleAfter = Duration.ofMinutes(10);
    private Duration sweepInterval = Duration.ofMinutes(5);
    /** After this many republishes without success the document is blocked instead of retried forever. */
    private int maxProcessingAttempts = 5;

    private final Storage storage = new Storage();
    private final Scanner scanner = new Scanner();

    /** The ClamAV daemon every upload is scanned by. */
    @Getter
    @Setter
    public static class Scanner {
        private String host = "localhost";
        private int port = 3310;
        private Duration connectTimeout = Duration.ofSeconds(3);
        private Duration readTimeout = Duration.ofSeconds(60);
    }

    @Getter
    @Setter
    public static class Storage {
        /** Blank means the real AWS S3; set it for SeaweedFS (dev) or Cloudflare R2. */
        private String endpoint;
        private String region = "us-east-1";
        private String accessKey;
        private String secretKey;
        private boolean pathStyleAccess = true;
        private String quarantineBucket = "quarantine";
        private String cleanBucket = "clean";
        private Duration presignTtl = Duration.ofMinutes(5);
    }
}
