package org.nanopub.testsuite;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TestSuiteDownloaderTest {

    @Test
    void downloadMainProducesExpectedStructure() {
        Path root = TestSuiteDownloader.download("main");

        assertTrue(Files.isDirectory(root), "Root directory should exist");
        assertTrue(Files.isDirectory(root.resolve("valid")), "valid/ should exist");
        assertTrue(Files.isDirectory(root.resolve("invalid")), "invalid/ should exist");
        assertTrue(Files.isDirectory(root.resolve("transform")), "transform/ should exist");
    }

    @Test
    void downloadMainProducesValidSubfolders() {
        Path root = TestSuiteDownloader.download("main");

        assertTrue(Files.isDirectory(root.resolve("valid/plain")), "valid/plain/ should exist");
        assertTrue(Files.isDirectory(root.resolve("valid/signed")), "valid/signed/ should exist");
        assertTrue(Files.isDirectory(root.resolve("valid/trusty")), "valid/trusty/ should exist");
    }

    @Test
    void downloadMainProducesInvalidSubfolders() {
        Path root = TestSuiteDownloader.download("main");

        assertTrue(Files.isDirectory(root.resolve("invalid/plain")), "invalid/plain/ should exist");
        assertTrue(Files.isDirectory(root.resolve("invalid/signed")), "invalid/signed/ should exist");
        assertTrue(Files.isDirectory(root.resolve("invalid/trusty")), "invalid/trusty/ should exist");
    }

    @Test
    void downloadMainProducesTransformStructure() {
        Path root = TestSuiteDownloader.download("main");

        assertTrue(Files.isDirectory(root.resolve("transform/plain")), "transform/plain/ should exist");
        assertTrue(Files.isDirectory(root.resolve("transform/signed/rsa-key1")), "transform/signed/rsa-key1/ should exist");
        assertTrue(Files.isDirectory(root.resolve("transform/signed/rsa-key2")), "transform/signed/rsa-key2/ should exist");
        assertTrue(Files.isDirectory(root.resolve("transform/signed/rsa-key1/key")), "rsa-key1/key/ should exist");
        assertTrue(Files.isDirectory(root.resolve("transform/signed/rsa-key2/key")), "rsa-key2/key/ should exist");
    }

    @Test
    void downloadMainProducesNanopubFiles() throws Exception {
        Path root = TestSuiteDownloader.download("main");

        try (Stream<Path> files = Files.list(root.resolve("valid/trusty"))) {
            long trigCount = files.filter(p -> p.getFileName().toString().endsWith(".trig")).count();
            assertTrue(trigCount > 0, "valid/trusty/ should contain .trig files");
        }
    }

    @Test
    void downloadMainProducesSigningKeys() throws Exception {
        Path root = TestSuiteDownloader.download("main");

        assertTrue(Files.isRegularFile(root.resolve("transform/signed/rsa-key1/key/id_rsa")), "rsa-key1 private key should exist");
        assertTrue(Files.isRegularFile(root.resolve("transform/signed/rsa-key1/key/id_rsa.pub")), "rsa-key1 public key should exist");
        assertTrue(Files.isRegularFile(root.resolve("transform/signed/rsa-key2/key/id_rsa")), "rsa-key2 private key should exist");
        assertTrue(Files.isRegularFile(root.resolve("transform/signed/rsa-key2/key/id_rsa.pub")), "rsa-key2 public key should exist");
        assertTrue(Files.size(root.resolve("transform/signed/rsa-key1/key/id_rsa")) > 0, "rsa-key1 private key should not be empty");
    }

    @Test
    void downloadMainProducesOutCodeFiles() throws Exception {
        Path root = TestSuiteDownloader.download("main");

        assertTrue(Files.isRegularFile(root.resolve("transform/signed/rsa-key1/simple1.out.code")));
        String code = Files.readString(root.resolve("transform/signed/rsa-key1/simple1.out.code")).strip();
        assertFalse(code.isBlank(), "out.code should not be blank");
        // artifact codes start with RA and are 45 chars
        assertTrue(code.startsWith("RA"), "artifact code should start with RA");
        assertEquals(45, code.length(), "artifact code should be 45 characters");
    }

    @Test
    void topLevelPrefixIsStripped() throws Exception {
        Path root = TestSuiteDownloader.download("main");

        try (Stream<Path> entries = Files.list(root)) {
            boolean hasUnstrippedPrefix = entries
                    .anyMatch(p -> p.getFileName().toString().startsWith("nanopub-testsuite-"));
            assertFalse(hasUnstrippedPrefix, "Top-level prefix should be stripped from extracted paths");
        }
    }

    @Test
    void invalidVersionThrowsRuntimeException() {
        assertThrows(RuntimeException.class, () -> TestSuiteDownloader.download("nonexistent-sha-000"));
    }

    @Test
    void downloadAtCommitProducesExpectedStructure() {
        Path root = TestSuiteDownloader.download("cbfd6e8");

        assertTrue(Files.isDirectory(root.resolve("valid")), "valid/ should exist");
        assertTrue(Files.isDirectory(root.resolve("invalid")), "invalid/ should exist");
        assertTrue(Files.isDirectory(root.resolve("transform")), "transform/ should exist");

        assertTrue(Files.isDirectory(root.resolve("valid/plain")), "valid/plain/ should exist");
        assertTrue(Files.isDirectory(root.resolve("valid/signed")), "valid/signed/ should exist");
        assertTrue(Files.isDirectory(root.resolve("valid/trusty")), "valid/trusty/ should exist");
        assertTrue(Files.isDirectory(root.resolve("invalid/plain")), "invalid/plain/ should exist");
        assertTrue(Files.isDirectory(root.resolve("invalid/signed")), "invalid/signed/ should exist");
        assertTrue(Files.isDirectory(root.resolve("invalid/trusty")), "invalid/trusty/ should exist");

        assertTrue(Files.isRegularFile(root.resolve("valid/signed/kpxl-nanopub-registry.trig")), "Expected valid/signed/kpxl-nanopub-registry.trig to exist since it was added in cbfd6e8 commit");
        assertFalse(Files.isRegularFile(root.resolve("invalid/trusty/trusty2.trig")), "Expected invalid/trusty/trusty2.trig to not exist since it was added after cbfd6e8 commit");
    }

}
