package com.sunday.services.processing;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ContentTypeDetectorTest {

    @Test
    void theRealTypeComesFromTheBytesNotTheName() {
        assertThat(ContentTypeDetector.detect(TestFiles.pdf())).contains(DetectedType.PDF);
        assertThat(ContentTypeDetector.detect(TestFiles.png(8, 8))).contains(DetectedType.PNG);
        assertThat(ContentTypeDetector.detect(TestFiles.jpeg(8, 8))).contains(DetectedType.JPEG);
    }

    @Test
    void anythingElseIsNotRecognised() {
        assertThat(ContentTypeDetector.detect(TestFiles.exe())).isEmpty();
        assertThat(ContentTypeDetector.detect("<html><script>alert(1)</script>".getBytes(StandardCharsets.UTF_8))).isEmpty();
        assertThat(ContentTypeDetector.detect("PK\u0003\u0004 zip or office".getBytes(StandardCharsets.ISO_8859_1))).isEmpty();
        assertThat(ContentTypeDetector.detect(new byte[0])).isEmpty();
        assertThat(ContentTypeDetector.detect(new byte[]{'%'})).isEmpty();
        assertThat(ContentTypeDetector.detect(null)).isEmpty();
    }

    @Test
    void aPdfHeaderMustBeAtTheVeryStart() {
        byte[] junkThenPdf = ("GIF89a-junk\n" + "%PDF-1.4\n").getBytes(StandardCharsets.US_ASCII);

        assertThat(ContentTypeDetector.detect(junkThenPdf)).isEmpty();
    }

    @Test
    void aSpecificDeclaredTypeThatDiffersFromTheRealOneIsACONTRADICTION() {
        assertThat(ContentTypeDetector.declaredTypeContradicts("image/png", DetectedType.PDF)).isTrue();
        assertThat(ContentTypeDetector.declaredTypeContradicts("application/x-msdownload", DetectedType.PDF)).isTrue();
        assertThat(ContentTypeDetector.declaredTypeContradicts("application/pdf", DetectedType.PNG)).isTrue();
    }

    @Test
    void aMissingGenericOrEquivalentDeclaredTypeIsNotAContradiction() {
        assertThat(ContentTypeDetector.declaredTypeContradicts(null, DetectedType.PDF)).isFalse();
        assertThat(ContentTypeDetector.declaredTypeContradicts(" ", DetectedType.PDF)).isFalse();
        assertThat(ContentTypeDetector.declaredTypeContradicts("application/octet-stream", DetectedType.PDF)).isFalse();
        assertThat(ContentTypeDetector.declaredTypeContradicts("application/pdf; charset=binary", DetectedType.PDF)).isFalse();
        assertThat(ContentTypeDetector.declaredTypeContradicts("APPLICATION/PDF", DetectedType.PDF)).isFalse();
        assertThat(ContentTypeDetector.declaredTypeContradicts("image/jpg", DetectedType.JPEG)).isFalse();
        assertThat(ContentTypeDetector.declaredTypeContradicts("application/x-pdf", DetectedType.PDF)).isFalse();
    }

    @Test
    void anExtensionThatDoesNotBelongToTheRealTypeIsAContradiction() {
        assertThat(ContentTypeDetector.extensionContradicts("licence.exe", DetectedType.PDF)).isTrue();
        assertThat(ContentTypeDetector.extensionContradicts("licence.png", DetectedType.PDF)).isTrue();
        assertThat(ContentTypeDetector.extensionContradicts("scan.pdf", DetectedType.JPEG)).isTrue();
    }

    @Test
    void matchingOrAbsentExtensionsAreFine() {
        assertThat(ContentTypeDetector.extensionContradicts("licence.pdf", DetectedType.PDF)).isFalse();
        assertThat(ContentTypeDetector.extensionContradicts("LICENCE.PDF", DetectedType.PDF)).isFalse();
        assertThat(ContentTypeDetector.extensionContradicts("photo.jpeg", DetectedType.JPEG)).isFalse();
        assertThat(ContentTypeDetector.extensionContradicts("photo.jpg", DetectedType.JPEG)).isFalse();
        assertThat(ContentTypeDetector.extensionContradicts("scan", DetectedType.PNG)).isFalse();
        assertThat(ContentTypeDetector.extensionContradicts("scan.", DetectedType.PNG)).isFalse();
        assertThat(ContentTypeDetector.extensionContradicts(null, DetectedType.PNG)).isFalse();
    }
}
