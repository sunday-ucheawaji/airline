package com.sunday.services.processing;

import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.config.DocumentProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Scans through a ClamAV daemon using its INSTREAM protocol: the command, then the file as length-prefixed chunks,
 * then a zero-length chunk; clamd answers {@code stream: OK} or {@code stream: <signature> FOUND}. Refuses to scan
 * while the daemon's virus definitions are older than {@code scanner.max-definition-age}, so a file is never passed
 * by a scanner that cannot recognise recent malware.
 */
@Component
public class ClamAvScanner implements MalwareScanner {

    private static final int CHUNK_SIZE = 64 * 1024;
    private static final String FOUND = " FOUND";
    private static final Duration DEFINITIONS_RECHECK = Duration.ofMinutes(10);
    private static final DateTimeFormatter CLAMAV_DATE = DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy", Locale.ENGLISH);

    private final DocumentProperties.Scanner settings;
    private final Clock clock;
    private volatile Instant definitionsCheckedUntil = Instant.MIN;

    @Autowired
    public ClamAvScanner(DocumentProperties properties) {
        this(properties, Clock.systemUTC());
    }

    ClamAvScanner(DocumentProperties properties, Clock clock) {
        this.settings = properties.getScanner();
        this.clock = clock;
    }

    @Override
    public ScanResult scan(byte[] content) {
        requireFreshDefinitions();
        try (Socket socket = connect()) {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            out.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));
            for (int offset = 0; offset < content.length; offset += CHUNK_SIZE) {
                int length = Math.min(CHUNK_SIZE, content.length - offset);
                out.writeInt(length);
                out.write(content, offset, length);
            }
            out.writeInt(0);
            out.flush();

            String reply = new String(socket.getInputStream().readAllBytes(), StandardCharsets.US_ASCII).replace("\0", "").strip();
            return interpret(reply);
        } catch (IOException e) {
            throw new ScannerUnavailableException("Malware scanner is not reachable", e);
        }
    }

    private void requireFreshDefinitions() {
        Instant now = clock.instant();
        if (now.isBefore(definitionsCheckedUntil)) {
            return;
        }
        Instant published = definitionsDate(version());
        if (published.isBefore(now.minus(settings.getMaxDefinitionAge()))) {
            throw new ScannerUnavailableException(String.format(ErrorMessageUtil.DOCUMENT_SCANNER_DEFINITIONS_STALE, published));
        }
        definitionsCheckedUntil = now.plus(DEFINITIONS_RECHECK);
    }

    private String version() {
        try (Socket socket = connect()) {
            socket.getOutputStream().write("zVERSION\0".getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.US_ASCII).replace("\0", "").strip();
        } catch (IOException e) {
            throw new ScannerUnavailableException("Malware scanner is not reachable", e);
        }
    }

    /** {@code ClamAV 1.4.1/27428/Sun Oct 12 08:30:00 2026}: the last part is when the loaded definitions were published. */
    static Instant definitionsDate(String versionReply) {
        String[] parts = versionReply.split("/");
        if (parts.length < 3) {
            throw new ScannerUnavailableException(ErrorMessageUtil.DOCUMENT_SCANNER_DEFINITIONS_UNKNOWN);
        }
        try {
            return LocalDateTime.parse(parts[2].strip().replaceAll("\\s+", " "), CLAMAV_DATE).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            throw new ScannerUnavailableException(ErrorMessageUtil.DOCUMENT_SCANNER_DEFINITIONS_UNKNOWN, e);
        }
    }

    private Socket connect() throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(settings.getHost(), settings.getPort()), (int) settings.getConnectTimeout().toMillis());
            socket.setSoTimeout((int) settings.getReadTimeout().toMillis());
            return socket;
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }

    static ScanResult interpret(String reply) {
        if (reply.endsWith("OK")) {
            return ScanResult.clean();
        }
        if (reply.endsWith(FOUND)) {
            String signature = reply.substring(0, reply.length() - FOUND.length());
            int colon = signature.indexOf(':');
            return ScanResult.infected(colon >= 0 ? signature.substring(colon + 1).strip() : signature.strip());
        }
        throw new ScannerUnavailableException("Malware scanner replied: " + reply);
    }
}
