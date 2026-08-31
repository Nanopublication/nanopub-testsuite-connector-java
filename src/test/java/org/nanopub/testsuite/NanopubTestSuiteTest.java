package org.nanopub.testsuite;

import net.trustyuri.ArtifactCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class NanopubTestSuiteTest {

    private static NanopubTestSuite suite;

    @BeforeAll
    static void load() {
        suite = NanopubTestSuite.getLatest();
    }

    @Test
    void validEntriesAreIndexed() {
        assertFalse(suite.getValid().isEmpty());
        assertFalse(suite.getValid(TestSuiteSubfolder.TRUSTY).isEmpty());
        assertFalse(suite.getValid(TestSuiteSubfolder.SIGNED).isEmpty());
        assertFalse(suite.getValid(TestSuiteSubfolder.PLAIN).isEmpty());
    }

    @Test
    void invalidEntriesAreIndexed() {
        assertFalse(suite.getInvalid().isEmpty());
    }

    @Test
    void transformCasesAreIndexed() {
        assertFalse(suite.getTransformCases("rsa-key1").isEmpty());
        assertFalse(suite.getTransformCases("rsa-key2").isEmpty());
    }

    @Test
    void trustyEntriesHaveArtifactCodes() {
        suite.getValid(TestSuiteSubfolder.TRUSTY).forEach(e ->
                assertNotNull(e.getArtifactCode(),
                        "Expected artifact code for trusty entry: " + e.getName()));
    }

    @Test
    void plainEntriesHaveNoArtifactCodes() {
        suite.getValid(TestSuiteSubfolder.PLAIN).forEach(e ->
                assertNull(e.getArtifactCode(),
                        "Expected no artifact code for plain entry: " + e.getName()));
    }

    @Test
    void lookupByKnownArtifactCode() {
        ArtifactCode artifactCode = ArtifactCode.of("RAPpJU5UOB4pavfWyk7FE3WQiam5yBpmIlviAQWtBSC4M");
        Optional<TestSuiteEntry> entry = suite.getByArtifactCode(artifactCode, TestSuiteCategory.VALID);
        assertTrue(entry.isPresent());
        assertEquals(TestSuiteSubfolder.TRUSTY, entry.get().getSubfolder());
    }

    @Test
    void lookupByNanopubUri() {
        Optional<TestSuiteEntry> entry = suite.getByNanopubUri("http://example.org/nanopub-validator-example/RAPpJU5UOB4pavfWyk7FE3WQiam5yBpmIlviAQWtBSC4M", TestSuiteCategory.VALID);
        assertTrue(entry.isPresent());
    }

    @Test
    void signingKeyStreamsAreReadable() throws Exception {
        SigningKeyPair key = suite.getSigningKey("rsa-key1");
        assertTrue(key.openPrivateKey().readAllBytes().length > 0);
        assertTrue(key.openPublicKey().readAllBytes().length > 0);
    }

    @Test
    void latestTracksMainAndIsCached() {
        assertEquals("main", suite.getVersion());
        assertTrue(suite.isLatest());
        assertSame(suite, NanopubTestSuite.getLatest(), "A version is downloaded once per JVM");
    }

    @Test
    void getAtCommitRejectsABlankSha() {
        assertThrows(IllegalArgumentException.class, () -> NanopubTestSuite.getAtCommit(null));
        assertThrows(IllegalArgumentException.class, () -> NanopubTestSuite.getAtCommit("   "));
    }

    @Test
    void getAtCommitPinsTheRequestedVersion() {
        NanopubTestSuite pinned = NanopubTestSuite.getAtCommit("cbfd6e8");

        assertEquals("cbfd6e8", pinned.getVersion());
        assertFalse(pinned.isLatest());
        assertFalse(pinned.getValid().isEmpty());
        assertSame(pinned, NanopubTestSuite.getAtCommit("cbfd6e8"), "A version is downloaded once per JVM");
    }

    @Test
    void everyKeyNameHasTransformCases() {
        List<String> keyNames = suite.getKeyNames();

        assertTrue(keyNames.containsAll(List.of("rsa-key1", "rsa-key2")), "Expected both RSA keys, got " + keyNames);
        assertEquals(keyNames.size(), keyNames.stream().distinct().count(), "Key names should not repeat");
        assertEquals(suite.getTransformCases().size(),
                keyNames.stream().mapToInt(k -> suite.getTransformCases(k).size()).sum());
    }

    @Test
    void invalidEntriesAreFilteredBySubfolder() {
        assertFalse(suite.getInvalid(TestSuiteSubfolder.TRUSTY).isEmpty());
        suite.getInvalid(TestSuiteSubfolder.TRUSTY).forEach(e ->
                assertEquals(TestSuiteSubfolder.TRUSTY, e.getSubfolder()));
    }

    @Test
    void entriesPointAtTheExtractedFile() {
        TestSuiteEntry entry = suite.getValid(TestSuiteSubfolder.TRUSTY).getFirst();

        assertTrue(entry.toFile().isFile(), "Expected a readable file for " + entry.getName());
        assertNotNull(entry.getNanopubUri());
    }

    @Test
    void signingKeysAreExposedAsFilesToo() {
        SigningKeyPair key = suite.getSigningKey("rsa-key1");

        assertEquals("rsa-key1", key.getKeyName());
        assertTrue(key.getPrivateKeyFile().isFile());
        assertTrue(key.getPublicKeyFile().isFile());
    }

    @Test
    void transformProfileIsAvailable() {
        File profile = suite.getTransformProfile();

        assertTrue(profile.isFile(), "Expected transform/profile.yaml in the extracted testsuite");
        assertEquals("profile.yaml", profile.getName());
    }

    @Test
    void transformCaseHasMatchingPlainAndCode() throws IOException {
        TransformTestCase tc = suite.getTransformCases("rsa-key1").getFirst();
        assertNotNull(tc.getPlainEntry().openStream());
        assertNotNull(tc.getExpectedCode());
        assertFalse(tc.getExpectedCode().isBlank());
        assertEquals("rsa-key1", tc.getKeyName());
        assertEquals(TestSuiteSubfolder.SIGNED, tc.getSignedEntry().getSubfolder());
        assertTrue(tc.getSignedEntry().toFile().isFile());
    }

}