package org.nanopub.testsuite;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Utility class to download and extract the nanopub testsuite from GitHub.
 */
class TestSuiteDownloader {

    private static final String REPO_TAR_URL = "https://github.com/Nanopublication/nanopub-testsuite/archive/%s.tar.gz";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    private TestSuiteDownloader() {
    }

    /**
     * Downloads and extracts the testsuite for the given version (commit SHA or "main")
     * into a temporary directory, and returns the path to that directory.
     */
    static Path download(String version) {
        try {
            Path dir = Files.createTempDirectory("nanopub-testsuite-" + version + "-");
            dir.toFile().deleteOnExit();
            Path tar = Files.createTempFile("nanopub-testsuite-", ".tar.gz");
            tar.toFile().deleteOnExit();
            fetch(version, tar);
            untarGz(tar, dir);
            Files.deleteIfExists(tar);
            return dir;
        } catch (IOException e) {
            throw new RuntimeException("Failed to download nanopub testsuite @ " + version, e);
        }
    }

    private static void fetch(String version, Path target) throws IOException {
        URLConnection conn = URI.create(String.format(REPO_TAR_URL, version)).toURL().openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void untarGz(Path tarGzFile, Path targetDir) throws IOException {
        try (InputStream fin = Files.newInputStream(tarGzFile);
             GzipCompressorInputStream gzIn = new GzipCompressorInputStream(fin);
             TarArchiveInputStream tais = new TarArchiveInputStream(gzIn)) {
            TarArchiveEntry entry;
            while ((entry = tais.getNextEntry()) != null) {
                String entryName = entry.getName().replaceFirst("^[^/]+/", "");
                if (entryName.isBlank()) {
                    continue;
                }
                Path outPath = targetDir.resolve(entryName);
                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Files.createDirectories(outPath.getParent());
                    Files.copy(tais, outPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
