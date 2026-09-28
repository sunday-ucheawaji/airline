package com.sunday.services.processing;

import com.sunday.services.config.DocumentProperties;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Scans through a ClamAV daemon using its INSTREAM protocol: the command, then the file as length-prefixed chunks,
 * then a zero-length chunk; clamd answers {@code stream: OK} or {@code stream: <signature> FOUND}.
 */
@Component
public class ClamAvScanner implements MalwareScanner {

    private static final int CHUNK_SIZE = 64 * 1024;
    private static final String FOUND = " FOUND";

    private final DocumentProperties.Scanner settings;

    public ClamAvScanner(DocumentProperties properties) {
        this.settings = properties.getScanner();
    }

    @Override
    public ScanResult scan(byte[] content) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(settings.getHost(), settings.getPort()), (int) settings.getConnectTimeout().toMillis());
            socket.setSoTimeout((int) settings.getReadTimeout().toMillis());

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
