package com.sunday.services.processing;

import com.sunday.services.config.DocumentProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The scanner against a small fake clamd that speaks the INSTREAM protocol. */
class ClamAvScannerTest {

    private FakeClamd clamd;

    @AfterEach
    void stop() throws IOException {
        if (clamd != null) {
            clamd.close();
        }
    }

    @Test
    void aCleanReplyMeansClean() throws Exception {
        clamd = new FakeClamd("stream: OK");

        MalwareScanner.ScanResult result = scanner(clamd.port()).scan("hello".getBytes(StandardCharsets.US_ASCII));

        assertThat(result.infected()).isFalse();
    }

    @Test
    void aFoundReplyNamesTheSignature() throws Exception {
        clamd = new FakeClamd("stream: Eicar-Test-Signature FOUND");

        MalwareScanner.ScanResult result = scanner(clamd.port()).scan(TestFiles.eicar());

        assertThat(result.infected()).isTrue();
        assertThat(result.signature()).isEqualTo("Eicar-Test-Signature");
    }

    @Test
    void theFileIsSentInFramedChunksExactlyAsReceivedByTheDaemon() throws Exception {
        byte[] content = new byte[200_000];
        new Random(3).nextBytes(content);
        clamd = new FakeClamd("stream: OK");

        scanner(clamd.port()).scan(content);

        assertThat(clamd.command()).isEqualTo("zINSTREAM\0");
        assertThat(clamd.chunkSizes()).hasSizeGreaterThan(1).allMatch(size -> size <= 64 * 1024);
        assertThat(clamd.received()).isEqualTo(content);
    }

    @Test
    void anErrorReplyIsNeverTreatedAsClean() throws Exception {
        clamd = new FakeClamd("INSTREAM size limit exceeded. ERROR");

        assertThatThrownBy(() -> scanner(clamd.port()).scan(new byte[10])).isInstanceOf(MalwareScanner.ScannerUnavailableException.class);
    }

    @Test
    void anUnreachableScannerIsNeverTreatedAsClean() throws Exception {
        int closedPort;
        try (ServerSocket probe = new ServerSocket(0)) {
            closedPort = probe.getLocalPort();
        }

        assertThatThrownBy(() -> scanner(closedPort).scan(new byte[10])).isInstanceOf(MalwareScanner.ScannerUnavailableException.class);
    }

    @Test
    void aScannerThatStopsAnsweringTimesOutInsteadOfHanging() throws Exception {
        clamd = new FakeClamd(null);

        assertThatThrownBy(() -> scanner(clamd.port()).scan(new byte[10])).isInstanceOf(MalwareScanner.ScannerUnavailableException.class);
    }

    private static ClamAvScanner scanner(int port) {
        DocumentProperties properties = new DocumentProperties();
        properties.getScanner().setHost("localhost");
        properties.getScanner().setPort(port);
        properties.getScanner().setConnectTimeout(Duration.ofSeconds(2));
        properties.getScanner().setReadTimeout(Duration.ofMillis(700));
        return new ClamAvScanner(properties);
    }

    /** Accepts one connection, records the INSTREAM exchange, and answers with a canned reply (or never answers when null). */
    private static final class FakeClamd implements AutoCloseable {
        private final ServerSocket server = new ServerSocket(0);
        private final CompletableFuture<Void> done = new CompletableFuture<>();
        private final ByteArrayOutputStream received = new ByteArrayOutputStream();
        private final List<Integer> chunkSizes = new ArrayList<>();
        private volatile String command;

        FakeClamd(String reply) throws IOException {
            Thread thread = new Thread(() -> {
                try (Socket socket = server.accept()) {
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    command = new String(in.readNBytes("zINSTREAM\0".length()), StandardCharsets.US_ASCII);
                    while (true) {
                        int length = in.readInt();
                        if (length == 0) {
                            break;
                        }
                        chunkSizes.add(length);
                        received.write(in.readNBytes(length));
                    }
                    if (reply != null) {
                        socket.getOutputStream().write((reply + "\0").getBytes(StandardCharsets.US_ASCII));
                        socket.getOutputStream().flush();
                    } else {
                        Thread.sleep(3_000);
                    }
                } catch (Exception ignored) {
                    // the test ends the connection
                } finally {
                    done.complete(null);
                }
            });
            thread.setDaemon(true);
            thread.start();
        }

        int port() {
            return server.getLocalPort();
        }

        String command() {
            return command;
        }

        List<Integer> chunkSizes() {
            return chunkSizes;
        }

        byte[] received() {
            return received.toByteArray();
        }

        @Override
        public void close() throws IOException {
            server.close();
        }
    }
}
