package com.sunday.services.storage;

import com.sunday.services.enums.DocumentType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class S3DocumentStorageTest {

    @Test
    void downloadNamesKeepOnlyCharactersThatAreSafeInAHeader() {
        assertThat(S3DocumentStorage.safeDownloadName("licence.pdf")).isEqualTo("licence.pdf");
        assertThat(S3DocumentStorage.safeDownloadName("my licence \"final\".pdf")).isEqualTo("my_licence__final_.pdf");
        assertThat(S3DocumentStorage.safeDownloadName("a\r\nSet-Cookie: x=1.pdf")).doesNotContain("\r", "\n", ":", " ");
        assertThat(S3DocumentStorage.safeDownloadName("../../etc/passwd")).doesNotContain("/");
    }

    @Test
    void anUnusableNameFallsBackToDocument() {
        assertThat(S3DocumentStorage.safeDownloadName(null)).isEqualTo("document");
        assertThat(S3DocumentStorage.safeDownloadName("")).isEqualTo("document");
        assertThat(S3DocumentStorage.safeDownloadName("....")).isEqualTo("document");
    }

    @Test
    void aVeryLongNameIsShortenedKeepingItsExtension() {
        String longName = "x".repeat(300) + ".pdf";

        String safe = S3DocumentStorage.safeDownloadName(longName);

        assertThat(safe).hasSizeLessThanOrEqualTo(100).endsWith(".pdf");
    }

    @Test
    void onlyTheCertificateOfIncorporationAndTheAirOperatorCertificateAreRequired() {
        assertThat(DocumentType.requiredTypes())
                .containsExactly(DocumentType.CERTIFICATE_OF_INCORPORATION, DocumentType.AIR_OPERATOR_CERTIFICATE);
    }
}
