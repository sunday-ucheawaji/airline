package com.sunday.services.processing;

import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.config.DocumentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PdfInspectorTest {

    private final DocumentProperties properties = new DocumentProperties();
    private PdfInspector inspector;

    @BeforeEach
    void setUp() {
        properties.setMaxPdfPages(5);
        inspector = new PdfInspector(properties);
    }

    @Test
    void anOrdinaryPdfIsAcceptedUnchanged() {
        byte[] pdf = TestFiles.pdf(3);

        InspectionOutcome outcome = inspector.inspect(pdf);

        assertThat(outcome.isBlocked()).isFalse();
        assertThat(outcome.content()).isEqualTo(pdf);
    }

    @Test
    void aPdfThatRunsJavaScriptWhenOpenedIsBlocked() {
        assertBlocked(TestFiles.pdfWithJavaScriptOpenAction(), ErrorMessageUtil.DOCUMENT_BLOCKED_PDF_ACTIVE_CONTENT);
    }

    @Test
    void aPdfThatLaunchesAProgramIsBlocked() {
        assertBlocked(TestFiles.pdfWithLaunchAnnotation(), ErrorMessageUtil.DOCUMENT_BLOCKED_PDF_ACTIVE_CONTENT);
    }

    @Test
    void aPdfCarryingEmbeddedFilesIsBlocked() {
        assertBlocked(TestFiles.pdfWithEmbeddedFiles(), ErrorMessageUtil.DOCUMENT_BLOCKED_PDF_ACTIVE_CONTENT);
    }

    @Test
    void aPdfWithAnXfaScriptedFormIsBlocked() {
        assertBlocked(TestFiles.pdfWithXfaForm(), ErrorMessageUtil.DOCUMENT_BLOCKED_PDF_ACTIVE_CONTENT);
    }

    @Test
    void aPasswordProtectedPdfIsBlocked() {
        assertBlocked(TestFiles.pdfWithUserPassword(), ErrorMessageUtil.DOCUMENT_BLOCKED_PDF_ENCRYPTED);
    }

    @Test
    void anEncryptedPdfIsBlockedEvenIfItOpensWithoutAPassword() {
        assertBlocked(TestFiles.pdfEncryptedWithEmptyUserPassword(), ErrorMessageUtil.DOCUMENT_BLOCKED_PDF_ENCRYPTED);
    }

    @Test
    void aPdfOverThePageLimitIsBlocked() {
        assertBlocked(TestFiles.pdf(6), ErrorMessageUtil.DOCUMENT_BLOCKED_PDF_TOO_MANY_PAGES);
        assertThat(inspector.inspect(TestFiles.pdf(5)).isBlocked()).isFalse();
    }

    @Test
    void aFileThatOnlyPretendsToBeAPdfIsBlockedAsUnreadable() {
        byte[] fake = "%PDF-1.7\nthis is not really a pdf, just text".getBytes(StandardCharsets.US_ASCII);

        assertBlocked(fake, ErrorMessageUtil.DOCUMENT_BLOCKED_PDF_UNREADABLE);
    }

    private void assertBlocked(byte[] pdf, String reason) {
        InspectionOutcome outcome = inspector.inspect(pdf);

        assertThat(outcome.isBlocked()).isTrue();
        assertThat(outcome.blockedReason()).isEqualTo(reason);
        assertThat(outcome.content()).isNull();
    }
}
