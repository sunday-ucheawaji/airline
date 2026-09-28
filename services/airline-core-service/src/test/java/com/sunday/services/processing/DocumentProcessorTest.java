package com.sunday.services.processing;

import com.sunday.common_lib.event.OnboardingDocumentUploadedEvent;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.config.DocumentProperties;
import com.sunday.services.enums.DocumentStatus;
import com.sunday.services.enums.DocumentType;
import com.sunday.services.model.AirlineOnboardingApplication;
import com.sunday.services.model.OnboardingDocument;
import com.sunday.services.repository.OnboardingDocumentRepository;
import com.sunday.services.storage.InMemoryDocumentStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentProcessorTest {

    private static final long APPLICATION = 10L;
    private static final long DOCUMENT = 77L;
    private static final String QUARANTINE_KEY = "onboarding/10/abc";

    @Mock OnboardingDocumentRepository documentRepository;
    @Mock MalwareScanner scanner;

    private final InMemoryDocumentStorage storage = new InMemoryDocumentStorage();
    private final DocumentProperties properties = new DocumentProperties();
    private DocumentProcessor processor;
    private OnboardingDocument document;

    @BeforeEach
    void setUp() {
        properties.setMaxPdfPages(5);
        processor = new DocumentProcessor(documentRepository, storage, scanner,
                new PdfInspector(properties), new ImageInspector(properties), properties);
        when(scanner.scan(any())).thenReturn(MalwareScanner.ScanResult.clean());
        when(documentRepository.save(any(OnboardingDocument.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ---------------------------------------------------------------- safe files

    @Test
    void aGenuinePdfBecomesCleanWithItsRealTypeSizeAndChecksum() throws Exception {
        byte[] pdf = TestFiles.pdf(2);
        upload("certificate.pdf", "application/pdf", pdf);

        processor.process(event());

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.CLEAN);
        assertThat(document.getDetectedContentType()).isEqualTo("application/pdf");
        assertThat(document.getFileSize()).isEqualTo(pdf.length);
        assertThat(document.getChecksumSha256()).isEqualTo(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pdf)));
        assertThat(document.getStorageBucket()).isEqualTo("clean");
        assertThat(document.getStorageKey()).startsWith("documents/10/").isNotEqualTo(QUARANTINE_KEY);
        assertThat(storage.clean.get(document.getStorageKey())).isEqualTo(pdf);
        assertThat(storage.cleanContentTypes.get(document.getStorageKey())).isEqualTo("application/pdf");
        assertThat(storage.quarantine).isEmpty();
    }

    @Test
    void anImageIsStoredAsTheReEncodedCopyNotTheOriginalUpload() throws Exception {
        byte[] upload = TestFiles.withAppended(TestFiles.png(40, 30), "<script>stealCookies()</script>");
        upload("scan.png", "image/png", upload);

        processor.process(event());

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.CLEAN);
        byte[] stored = storage.clean.get(document.getStorageKey());
        assertThat(new String(stored, StandardCharsets.ISO_8859_1)).doesNotContain("stealCookies");
        assertThat(document.getFileSize()).isEqualTo(stored.length);
        assertThat(document.getChecksumSha256()).isEqualTo(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(stored)));
        assertThat(document.getDetectedContentType()).isEqualTo("image/png");
    }

    @Test
    void aGenericOrMissingDeclaredTypeDoesNotStopAGenuineFile() {
        upload("certificate.pdf", "application/octet-stream", TestFiles.pdf());
        processor.process(event());
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.CLEAN);

        upload("certificate.pdf", null, TestFiles.pdf());
        processor.process(event());
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.CLEAN);
    }

    // ---------------------------------------------------------------- files that pretend

    @Test
    void anExecutableRenamedAsAPdfIsBlockedAndNeverScannedOrKept() {
        upload("licence.pdf", "application/pdf", TestFiles.exe());

        processor.process(event());

        assertBlocked(ErrorMessageUtil.DOCUMENT_BLOCKED_TYPE_NOT_ALLOWED);
        verify(scanner, never()).scan(any());
    }

    @Test
    void aFileWhoseContentContradictsItsDeclaredTypeIsBlocked() {
        upload("photo.png", "image/png", TestFiles.pdf());

        processor.process(event());

        assertBlocked(ErrorMessageUtil.DOCUMENT_BLOCKED_TYPE_MISMATCH);
    }

    @Test
    void aGenuinePdfWithAnExecutableExtensionIsBlocked() {
        upload("licence.exe", "application/pdf", TestFiles.pdf());

        processor.process(event());

        assertBlocked(ErrorMessageUtil.DOCUMENT_BLOCKED_TYPE_MISMATCH);
    }

    @Test
    void htmlOrScriptsAreNotAllowedAtAll() {
        upload("page.pdf", "application/pdf", "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8));

        processor.process(event());

        assertBlocked(ErrorMessageUtil.DOCUMENT_BLOCKED_TYPE_NOT_ALLOWED);
    }

    // ---------------------------------------------------------------- structure

    @Test
    void aPdfWithActiveContentIsBlocked() {
        upload("form.pdf", "application/pdf", TestFiles.pdfWithJavaScriptOpenAction());

        processor.process(event());

        assertBlocked(ErrorMessageUtil.DOCUMENT_BLOCKED_PDF_ACTIVE_CONTENT);
        verify(scanner, never()).scan(any());
    }

    @Test
    void anEncryptedPdfIsBlocked() {
        upload("secret.pdf", "application/pdf", TestFiles.pdfWithUserPassword());

        processor.process(event());

        assertBlocked(ErrorMessageUtil.DOCUMENT_BLOCKED_PDF_ENCRYPTED);
    }

    @Test
    void aBrokenImageIsBlocked() {
        byte[] png = TestFiles.png(40, 30);
        upload("scan.png", "image/png", java.util.Arrays.copyOf(png, png.length / 2));

        processor.process(event());

        assertBlocked(ErrorMessageUtil.DOCUMENT_BLOCKED_IMAGE_UNREADABLE);
    }

    // ---------------------------------------------------------------- malware

    @Test
    void aFileTheScannerFlagsIsBlockedAndRemoved() {
        when(scanner.scan(any())).thenReturn(MalwareScanner.ScanResult.infected("Eicar-Test-Signature"));
        upload("certificate.pdf", "application/pdf", TestFiles.pdf());

        processor.process(event());

        assertBlocked(ErrorMessageUtil.DOCUMENT_BLOCKED_MALWARE);
    }

    @Test
    void theOriginalUploadIsWhatGetsScanned() {
        byte[] upload = TestFiles.withAppended(TestFiles.png(40, 30), "appended");
        upload("scan.png", "image/png", upload);

        processor.process(event());

        verify(scanner).scan(upload);
    }

    // ---------------------------------------------------------------- outages fail closed

    @Test
    void aScannerOutageLeavesTheDocumentProcessingAndTheFileInQuarantine() {
        when(scanner.scan(any())).thenThrow(new MalwareScanner.ScannerUnavailableException("down"));
        upload("certificate.pdf", "application/pdf", TestFiles.pdf());

        assertThatThrownBy(() -> processor.process(event())).isInstanceOf(MalwareScanner.ScannerUnavailableException.class);

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.PROCESSING);
        assertThat(storage.quarantine).containsKey(QUARANTINE_KEY);
        assertThat(storage.clean).isEmpty();
        verify(documentRepository, never()).save(any(OnboardingDocument.class));
    }

    @Test
    void aStorageOutageIsRetriedNotBlocked() {
        upload("certificate.pdf", "application/pdf", TestFiles.pdf());
        storage.unavailable = true;

        assertThatThrownBy(() -> processor.process(event())).isInstanceOf(ServiceUnavailableException.class);

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.PROCESSING);
        verify(documentRepository, never()).save(any(OnboardingDocument.class));
    }

    @Test
    void ifTheRecordCannotBeSavedTheCleanCopyIsRemovedAndTheQuarantineKept() {
        when(documentRepository.save(any(OnboardingDocument.class))).thenThrow(new IllegalStateException("database down"));
        upload("certificate.pdf", "application/pdf", TestFiles.pdf());

        assertThatThrownBy(() -> processor.process(event())).isInstanceOf(IllegalStateException.class);

        assertThat(storage.clean).isEmpty();
        assertThat(storage.quarantine).containsKey(QUARANTINE_KEY);
    }

    // ---------------------------------------------------------------- idempotence and limits

    @Test
    void aDocumentThatIsNoLongerProcessingIsLeftAlone() {
        upload("certificate.pdf", "application/pdf", TestFiles.pdf());
        document.setStatus(DocumentStatus.CLEAN);

        processor.process(event());

        verify(scanner, never()).scan(any());
        verify(documentRepository, never()).save(any(OnboardingDocument.class));
    }

    @Test
    void anEventForAnUnknownDocumentIsIgnored() {
        when(documentRepository.findById(DOCUMENT)).thenReturn(Optional.empty());

        processor.process(event());

        verify(documentRepository, never()).save(any(OnboardingDocument.class));
    }

    @Test
    void aFileLargerThanTheLimitFoundInQuarantineIsBlockedWithoutBeingFullyRead() {
        properties.setMaxFileSizeBytes(10);
        upload("certificate.pdf", "application/pdf", TestFiles.pdf());

        processor.process(event());

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.BLOCKED);
        assertThat(document.getBlockedReason()).contains("maximum size");
        assertThat(storage.quarantine).isEmpty();
    }

    // ---------------------------------------------------------------- helpers

    private void assertBlocked(String reason) {
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.BLOCKED);
        assertThat(document.getBlockedReason()).isEqualTo(reason);
        assertThat(storage.quarantine).isEmpty();
        assertThat(storage.clean).isEmpty();
    }

    private void upload(String fileName, String declaredContentType, byte[] content) {
        AirlineOnboardingApplication application = new AirlineOnboardingApplication();
        application.setId(APPLICATION);
        storage.quarantine.clear();
        storage.putQuarantine(QUARANTINE_KEY, new ByteArrayInputStream(content), content.length);
        document = OnboardingDocument.builder().id(DOCUMENT).application(application).documentType(DocumentType.CERTIFICATE_OF_INCORPORATION)
                .originalFileName(fileName).declaredContentType(declaredContentType).storageProvider("MEMORY")
                .storageBucket("quarantine").storageKey(QUARANTINE_KEY).uploadedByUserId(1L).build();
        when(documentRepository.findById(DOCUMENT)).thenReturn(Optional.of(document));
    }

    private static OnboardingDocumentUploadedEvent event() {
        return OnboardingDocumentUploadedEvent.builder().documentId(DOCUMENT).applicationId(APPLICATION).quarantineKey(QUARANTINE_KEY).build();
    }
}
