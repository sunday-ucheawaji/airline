package com.sunday.services.processing;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionLaunch;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

/** Builds real benign and hostile files for the inspection tests. */
final class TestFiles {

    private TestFiles() {
    }

    static byte[] pdf() {
        return pdf(1, d -> { });
    }

    static byte[] pdf(int pages) {
        return pdf(pages, d -> { });
    }

    static byte[] pdfWithJavaScriptOpenAction() {
        return pdf(1, d -> d.getDocumentCatalog().setOpenAction(new PDActionJavaScript("app.alert('x');")));
    }

    static byte[] pdfWithLaunchAnnotation() {
        return pdf(1, d -> {
            try {
                PDAnnotationLink link = new PDAnnotationLink();
                link.setAction(new PDActionLaunch());
                List<PDAnnotation> annotations = d.getPage(0).getAnnotations();
                annotations.add(link);
                d.getPage(0).setAnnotations(annotations);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    static byte[] pdfWithEmbeddedFiles() {
        return pdf(1, d -> {
            COSDictionary names = new COSDictionary();
            names.setItem(COSName.getPDFName("EmbeddedFiles"), new COSDictionary());
            d.getDocumentCatalog().getCOSObject().setItem(COSName.getPDFName("Names"), names);
        });
    }

    static byte[] pdfWithXfaForm() {
        return pdf(1, d -> {
            COSDictionary form = new COSDictionary();
            form.setItem(COSName.getPDFName("XFA"), COSName.getPDFName("form"));
            d.getDocumentCatalog().getCOSObject().setItem(COSName.getPDFName("AcroForm"), form);
        });
    }

    /** Needs a password to open at all. */
    static byte[] pdfWithUserPassword() {
        return pdf(1, d -> protect(d, "user-secret"));
    }

    /** Opens without a password but is still encrypted. */
    static byte[] pdfEncryptedWithEmptyUserPassword() {
        return pdf(1, d -> protect(d, ""));
    }

    static byte[] png(int width, int height) {
        return image("png", width, height);
    }

    static byte[] jpeg(int width, int height) {
        return image("jpeg", width, height);
    }

    /** A Windows executable header: what an attacker renames to look like a document. */
    static byte[] exe() {
        byte[] body = "This program cannot be run in DOS mode.".getBytes(StandardCharsets.US_ASCII);
        byte[] exe = new byte[2 + body.length];
        exe[0] = 'M';
        exe[1] = 'Z';
        System.arraycopy(body, 0, exe, 2, body.length);
        return exe;
    }

    /** The standard anti-virus test string, assembled at run time so this source file itself is not flagged. */
    static byte[] eicar() {
        String text = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$" + "EICAR-STANDARD-ANTIVIRUS-TEST-FILE!" + "$H+H*";
        return text.getBytes(StandardCharsets.US_ASCII);
    }

    static byte[] withAppended(byte[] file, String payload) {
        byte[] extra = payload.getBytes(StandardCharsets.UTF_8);
        byte[] combined = new byte[file.length + extra.length];
        System.arraycopy(file, 0, combined, 0, file.length);
        System.arraycopy(extra, 0, combined, file.length, extra.length);
        return combined;
    }

    private static byte[] pdf(int pages, Consumer<PDDocument> customise) {
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < pages; i++) {
                document.addPage(new PDPage());
            }
            customise.accept(document);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void protect(PDDocument document, String userPassword) {
        try {
            StandardProtectionPolicy policy = new StandardProtectionPolicy("owner-secret", userPassword, new AccessPermission());
            policy.setEncryptionKeyLength(128);
            document.protect(policy);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] image(String format, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(width / 4, height / 4, width / 2, height / 2);
        graphics.dispose();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, format, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
