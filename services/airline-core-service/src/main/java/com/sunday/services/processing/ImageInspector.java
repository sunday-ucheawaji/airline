package com.sunday.services.processing;

import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.config.DocumentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

/**
 * Decodes an image completely and writes it back out as a fresh file. Decoding proves it really is an image of the
 * claimed format; re-encoding discards everything that is not pixels (metadata, comments, and any payload appended
 * to or hidden inside the file), so what is stored is only what was decoded. Dimensions are read from the header first
 * so a decompression bomb is refused before any pixel memory is allocated.
 */
@Component
@RequiredArgsConstructor
public class ImageInspector {

    private static final float JPEG_QUALITY = 0.95f;

    private final DocumentProperties properties;

    public InspectionOutcome inspect(byte[] content, DetectedType type) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return InspectionOutcome.blocked(ErrorMessageUtil.DOCUMENT_BLOCKED_IMAGE_UNREADABLE);
            }
            ImageReader reader = readers.next();
            try {
                if (!formatMatches(reader, type)) {
                    return InspectionOutcome.blocked(ErrorMessageUtil.DOCUMENT_BLOCKED_TYPE_MISMATCH);
                }
                reader.setInput(input, true, true);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels <= 0 || pixels > properties.getMaxImagePixels()) {
                    return InspectionOutcome.blocked(ErrorMessageUtil.DOCUMENT_BLOCKED_IMAGE_TOO_LARGE);
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    return InspectionOutcome.blocked(ErrorMessageUtil.DOCUMENT_BLOCKED_IMAGE_UNREADABLE);
                }
                byte[] rewritten = type == DetectedType.PNG ? writePng(image) : writeJpeg(image);
                return rewritten == null
                        ? InspectionOutcome.blocked(ErrorMessageUtil.DOCUMENT_BLOCKED_IMAGE_UNREADABLE)
                        : InspectionOutcome.accepted(rewritten);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            return InspectionOutcome.blocked(ErrorMessageUtil.DOCUMENT_BLOCKED_IMAGE_UNREADABLE);
        }
    }

    private static boolean formatMatches(ImageReader reader, DetectedType type) throws IOException {
        String format = reader.getFormatName().toLowerCase(Locale.ROOT);
        return type == DetectedType.PNG ? format.equals("png") : format.equals("jpeg") || format.equals("jpg");
    }

    private static byte[] writePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        return ImageIO.write(image, "png", out) ? out.toByteArray() : null;
    }

    private static byte[] writeJpeg(BufferedImage image) throws IOException {
        // JPEG has no transparency: flatten onto white so the writer always accepts the image.
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            return null;
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream imageOut = ImageIO.createImageOutputStream(out)) {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.setOutput(imageOut);
            writer.write(null, new IIOImage(rgb, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }
}
