package org.nanopub.testsuite;

import net.trustyuri.ArtifactCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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
    void transformCaseHasMatchingPlainAndCode() throws IOException {
        TransformTestCase tc = suite.getTransformCases("rsa-key1").getFirst();
        assertNotNull(tc.getPlainEntry().openStream());
        assertNotNull(tc.getExpectedCode());
        assertFalse(tc.getExpectedCode().isBlank());
    }

}