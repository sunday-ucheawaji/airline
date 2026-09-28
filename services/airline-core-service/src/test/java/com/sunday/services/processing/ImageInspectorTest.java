package com.sunday.services.processing;

import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.config.DocumentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class ImageInspectorTest {

    private static final String PAYLOAD = "<script>stealCookies()</script>";

    private final DocumentProperties properties = new DocumentProperties();
    private ImageInspector inspector;

    @BeforeEach
    void setUp() {
        properties.setMaxImagePixels(10_000);
        inspector = new ImageInspector(properties);
    }

    @Test
    void aRealPngIsAcceptedAndComesBackAsAFreshPng() throws IOException {
        InspectionOutcome outcome = inspector.inspect(TestFiles.png(40, 30), DetectedType.PNG);

        assertThat(outcome.isBlocked()).isFalse();
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(outcome.content()));
        assertThat(decoded.getWidth()).isEqualTo(40);
        assertThat(decoded.getHeight()).isEqualTo(30);
        assertThat(ContentTypeDetector.detect(outcome.content())).contains(DetectedType.PNG);
    }

    @Test
    void aRealJpegIsAcceptedAndComesBackAsAFreshJpeg() throws IOException {
        InspectionOutcome outcome = inspector.inspect(TestFiles.jpeg(40, 30), DetectedType.JPEG);

        assertThat(outcome.isBlocked()).isFalse();
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(outcome.content()));
        assertThat(decoded.getWidth()).isEqualTo(40);
        assertThat(ContentTypeDetector.detect(outcome.content())).contains(DetectedType.JPEG);
    }

    @Test
    void dataAppendedAfterAnImageIsStrippedByTheRewrite() {
        byte[] png = TestFiles.withAppended(TestFiles.png(40, 30), PAYLOAD);
        byte[] jpeg = TestFiles.withAppended(TestFiles.jpeg(40, 30), PAYLOAD);

        InspectionOutcome fromPng = inspector.inspect(png, DetectedType.PNG);
        InspectionOutcome fromJpeg = inspector.inspect(jpeg, DetectedType.JPEG);

        assertThat(fromPng.isBlocked()).isFalse();
        assertThat(fromJpeg.isBlocked()).isFalse();
        assertThat(new String(fromPng.content(), StandardCharsets.ISO_8859_1)).doesNotContain("stealCookies");
        assertThat(new String(fromJpeg.content(), StandardCharsets.ISO_8859_1)).doesNotContain("stealCookies");
    }

    @Test
    void aFileWithAPngHeaderButGarbageInsideIsBlockedAsUnreadable() {
        byte[] garbage = new byte[200];
        new Random(7).nextBytes(garbage);
        System.arraycopy(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}, 0, garbage, 0, 8);

        InspectionOutcome outcome = inspector.inspect(garbage, DetectedType.PNG);

        assertThat(outcome.isBlocked()).isTrue();
        assertThat(outcome.blockedReason()).isEqualTo(ErrorMessageUtil.DOCUMENT_BLOCKED_IMAGE_UNREADABLE);
    }

    @Test
    void aTruncatedImageIsBlocked() {
        byte[] png = TestFiles.png(40, 30);

        InspectionOutcome outcome = inspector.inspect(Arrays.copyOf(png, png.length / 2), DetectedType.PNG);

        assertThat(outcome.isBlocked()).isTrue();
    }

    @Test
    void anImageWithTooManyPixelsIsRefusedBeforeItIsDecoded() {
        InspectionOutcome outcome = inspector.inspect(TestFiles.png(200, 200), DetectedType.PNG);

        assertThat(outcome.isBlocked()).isTrue();
        assertThat(outcome.blockedReason()).isEqualTo(ErrorMessageUtil.DOCUMENT_BLOCKED_IMAGE_TOO_LARGE);
    }

    @Test
    void aJpegPresentedAsAPngIsBlockedAsAMismatch() {
        InspectionOutcome outcome = inspector.inspect(TestFiles.jpeg(40, 30), DetectedType.PNG);

        assertThat(outcome.isBlocked()).isTrue();
        assertThat(outcome.blockedReason()).isEqualTo(ErrorMessageUtil.DOCUMENT_BLOCKED_TYPE_MISMATCH);
    }
}
