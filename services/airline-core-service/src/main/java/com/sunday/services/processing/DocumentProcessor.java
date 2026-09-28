package com.sunday.services.processing;

import com.sunday.common_lib.event.OnboardingDocumentUploadedEvent;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.config.DocumentProperties;
import com.sunday.services.enums.DocumentStatus;
import com.sunday.services.model.OnboardingDocument;
import com.sunday.services.repository.OnboardingDocumentRepository;
import com.sunday.services.storage.DocumentStorage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * The asynchronous inspection of an uploaded document. Reads the raw file from quarantine, decides what it really is
 * from its contents, checks its structure, scans it for malware, and then either promotes a safe (possibly re-encoded)
 * copy to clean storage or blocks the document. Nothing is decided from the name or type the uploader claimed.
 *
 * <p>Idempotent: a document that has left PROCESSING is skipped, so Kafka's at-least-once delivery and the sweeper's
 * republishing are harmless. A storage or scanner outage throws, so the message is retried; the document is never
 * marked safe without a scan.
 */
@Service
@RequiredArgsConstructor
public class DocumentProcessor {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessor.class);

    private final OnboardingDocumentRepository documentRepository;
    private final DocumentStorage storage;
    private final MalwareScanner scanner;
    private final PdfInspector pdfInspector;
    private final ImageInspector imageInspector;
    private final DocumentProperties properties;

    public void process(OnboardingDocumentUploadedEvent event) {
        Optional<OnboardingDocument> found = documentRepository.findById(event.getDocumentId());
        if (found.isEmpty() || found.get().getStatus() != DocumentStatus.PROCESSING) {
            log.info("Skipping document {}: already processed or unknown", event.getDocumentId());
            return;
        }
        OnboardingDocument document = found.get();

        byte[] original = readQuarantined(document);
        if (original == null) {
            block(document, String.format(ErrorMessageUtil.DOCUMENT_TOO_LARGE, properties.getMaxFileSizeBytes()), null);
            return;
        }

        Optional<DetectedType> detected = ContentTypeDetector.detect(original);
        if (detected.isEmpty()) {
            block(document, ErrorMessageUtil.DOCUMENT_BLOCKED_TYPE_NOT_ALLOWED, sha256(original));
            return;
        }
        DetectedType type = detected.get();
        if (ContentTypeDetector.declaredTypeContradicts(document.getDeclaredContentType(), type)
                || ContentTypeDetector.extensionContradicts(document.getOriginalFileName(), type)) {
            block(document, ErrorMessageUtil.DOCUMENT_BLOCKED_TYPE_MISMATCH, sha256(original));
            return;
        }

        InspectionOutcome inspected = type == DetectedType.PDF ? pdfInspector.inspect(original) : imageInspector.inspect(original, type);
        if (inspected.isBlocked()) {
            block(document, inspected.blockedReason(), sha256(original));
            return;
        }

        // Scan what was uploaded, not the re-encoded copy: re-encoding could remove the very bytes that give malware away.
        MalwareScanner.ScanResult scan = scanner.scan(original);
        if (scan.infected()) {
            log.warn("Document {} blocked: malware signature {}", document.getId(), scan.signature());
            block(document, ErrorMessageUtil.DOCUMENT_BLOCKED_MALWARE, sha256(original));
            return;
        }

        promote(document, event, type, inspected.content());
    }

    private void promote(OnboardingDocument document, OnboardingDocumentUploadedEvent event, DetectedType type, byte[] content) {
        String previousKey = document.getStorageKey();
        String cleanKey = "documents/" + event.getApplicationId() + "/" + UUID.randomUUID();
        storage.putClean(cleanKey, new ByteArrayInputStream(content), content.length, type.mimeType());
        try {
            document.setStatus(DocumentStatus.CLEAN);
            document.setDetectedContentType(type.mimeType());
            document.setFileSize((long) content.length);
            document.setChecksumSha256(sha256(content));
            document.setStorageBucket(storage.cleanBucket());
            document.setStorageKey(cleanKey);
            document.setBlockedReason(null);
            documentRepository.save(document);
        } catch (RuntimeException e) {
            removeQuietly(() -> storage.deleteClean(cleanKey), cleanKey);
            throw e;
        }
        removeQuietly(() -> storage.deleteQuarantine(previousKey), previousKey);
        log.info("Document {} passed inspection as {}", document.getId(), type.mimeType());
    }

    private void block(OnboardingDocument document, String reason, String sha256OfUpload) {
        log.warn("Document {} blocked: {} (sha256 of upload: {})", document.getId(), reason, sha256OfUpload);
        String quarantineKey = document.getStorageKey();
        document.setStatus(DocumentStatus.BLOCKED);
        document.setBlockedReason(reason);
        documentRepository.save(document);
        removeQuietly(() -> storage.deleteQuarantine(quarantineKey), quarantineKey);
    }

    /** Reads the quarantined file, or returns null if it is larger than the limit (never buffers more than limit + 1 bytes). */
    private byte[] readQuarantined(OnboardingDocument document) {
        long limit = properties.getMaxFileSizeBytes();
        try (InputStream in = storage.openQuarantine(document.getStorageKey())) {
            byte[] bytes = in.readNBytes((int) Math.min(limit + 1, Integer.MAX_VALUE));
            return bytes.length > limit ? null : bytes;
        } catch (IOException e) {
            throw new com.sunday.common_lib.exception.ServiceUnavailableException(ErrorMessageUtil.DOCUMENT_STORAGE_UNAVAILABLE, e);
        }
    }

    private void removeQuietly(Runnable deletion, String key) {
        try {
            deletion.run();
        } catch (RuntimeException e) {
            log.warn("Could not remove stored object {}", key, e);
        }
    }

    static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
