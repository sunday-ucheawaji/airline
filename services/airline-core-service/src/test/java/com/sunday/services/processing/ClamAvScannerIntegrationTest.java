package com.sunday.services.processing;

import com.sunday.services.config.DocumentProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.Socket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Talks to the real ClamAV from docker-compose.dev.yml. Skipped when it is not running, so the normal build never
 * depends on Docker. Override the address with DOCUMENT_SCANNER_HOST / DOCUMENT_SCANNER_PORT.
 */
class ClamAvScannerIntegrationTest {

    private static ClamAvScanner scanner;

    @BeforeAll
    static void connect() {
        String host = System.getenv().getOrDefault("DOCUMENT_SCANNER_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("DOCUMENT_SCANNER_PORT", "3310"));
        assumeTrue(reachable(host, port), "ClamAV not running at " + host + ":" + port);

        DocumentProperties properties = new DocumentProperties();
        properties.getScanner().setHost(host);
        properties.getScanner().setPort(port);
        scanner = new ClamAvScanner(properties);
    }

    @Test
    void anOrdinaryFileIsClean() {
        assertThat(scanner.scan(TestFiles.pdf()).infected()).isFalse();
        assertThat(scanner.scan(TestFiles.png(30, 30)).infected()).isFalse();
    }

    @Test
    void theStandardTestVirusIsDetectedByName() {
        MalwareScanner.ScanResult result = scanner.scan(TestFiles.eicar());

        assertThat(result.infected()).isTrue();
        assertThat(result.signature()).containsIgnoringCase("eicar");
    }

    private static boolean reachable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
