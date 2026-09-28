package com.sunday.services.processing;

import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.config.DocumentProperties;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Fully parses a PDF and blocks it if it is unreadable, encrypted, too long, or carries active content. Active content
 * is looked for by walking every object reachable from the document (including those packed in compressed object
 * streams), not by searching raw bytes, so compression cannot hide it. The file is stored unchanged when it passes.
 */
@Component
@RequiredArgsConstructor
public class PdfInspector {

    /** Keys that only appear in scripts, embedded files, rich media or XFA forms. */
    private static final Set<String> BLOCKED_KEYS = Set.of("JS", "JavaScript", "EmbeddedFiles", "EmbeddedFile", "EF", "RichMedia", "XFA");
    /** Actions (the {@code /S} entry) that run code, launch programs, or exchange data. */
    private static final Set<String> BLOCKED_ACTIONS = Set.of(
            "JavaScript", "Launch", "ImportData", "SubmitForm", "Rendition", "Movie", "Sound", "GoToE", "RichMediaExecute");
    /** A malicious file can have an enormous object graph; anything beyond this is refused instead of traversed. */
    private static final int MAX_OBJECTS = 250_000;

    private final DocumentProperties properties;

    public InspectionOutcome inspect(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            if (document.isEncrypted()) {
                return InspectionOutcome.blocked(ErrorMessageUtil.DOCUMENT_BLOCKED_PDF_ENCRYPTED);
            }
            if (document.getNumberOfPages() > properties.getMaxPdfPages()) {
                return InspectionOutcome.blocked(ErrorMessageUtil.DOCUMENT_BLOCKED_PDF_TOO_MANY_PAGES);
            }
            return switch (activeContent(document.getDocument().getTrailer())) {
                case FOUND -> InspectionOutcome.blocked(ErrorMessageUtil.DOCUMENT_BLOCKED_PDF_ACTIVE_CONTENT);
                case TOO_COMPLEX -> InspectionOutcome.blocked(ErrorMessageUtil.DOCUMENT_BLOCKED_PDF_UNREADABLE);
                case NONE -> InspectionOutcome.accepted(content);
            };
        } catch (InvalidPasswordException e) {
            return InspectionOutcome.blocked(ErrorMessageUtil.DOCUMENT_BLOCKED_PDF_ENCRYPTED);
        } catch (IOException | RuntimeException e) {
            return InspectionOutcome.blocked(ErrorMessageUtil.DOCUMENT_BLOCKED_PDF_UNREADABLE);
        }
    }

    private enum Scan { NONE, FOUND, TOO_COMPLEX }

    private static Scan activeContent(COSDictionary trailer) throws IOException {
        Deque<COSBase> pending = new ArrayDeque<>();
        Map<COSBase, Boolean> seen = new IdentityHashMap<>();
        pending.push(trailer);
        while (!pending.isEmpty()) {
            COSBase node = pending.pop();
            if (node instanceof COSObject object) {
                node = object.getObject();
            }
            if (node == null || seen.put(node, Boolean.TRUE) != null) {
                continue;
            }
            if (seen.size() > MAX_OBJECTS) {
                return Scan.TOO_COMPLEX;
            }
            if (node instanceof COSDictionary dictionary) {
                if (isSuspicious(dictionary)) {
                    return Scan.FOUND;
                }
                dictionary.getValues().forEach(pending::push);
            } else if (node instanceof COSArray array) {
                array.forEach(pending::push);
            }
        }
        return Scan.NONE;
    }

    private static boolean isSuspicious(COSDictionary dictionary) {
        for (COSName key : dictionary.keySet()) {
            if (BLOCKED_KEYS.contains(key.getName())) {
                return true;
            }
        }
        COSBase action = dictionary.getDictionaryObject(COSName.S);
        return action instanceof COSName name && BLOCKED_ACTIONS.contains(name.getName());
    }

    /** Test hook: the set of names treated as active, so tests can reason about what is covered. */
    static Set<String> blockedKeys() {
        return Collections.unmodifiableSet(BLOCKED_KEYS);
    }
}
