package org.nanopub.testsuite;

import net.trustyuri.ArtifactCode;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Indexing behaviour exercised against hand-built trees rather than the downloaded
 * testsuite. The real testsuite is well-formed by construction, so the paths that
 * matter here — a file that doesn't parse, a nanopub-less graph, a missing
 * {@code .out.code}, a directory that cannot be listed — never occur in it, yet they
 * decide what the connector does when the testsuite grows a case like that.
 */
class NanopubTestSuiteFixtureTest {

    private static final String NP_TYPE = "http://www.nanopub.org/nschema#Nanopublication";
    private static final String TRUSTY_CODE = "RAPpJU5UOB4pavfWyk7FE3WQiam5yBpmIlviAQWtBSC4M";
    private static final String TRUSTY_URI = "http://example.org/nanopub/" + TRUSTY_CODE;
    // Artifact codes are the module id plus a 43-character hash; any such string is a
    // well-formed code, and these two stand for nanopubs the fixture indexes elsewhere
    // or not at all.
    private static final String TRANSFORM_CODE = "RA" + "b".repeat(43);
    private static final String TRANSFORM_URI = "http://example.org/nanopub/" + TRANSFORM_CODE;
    private static final String UNKNOWN_CODE = "RA" + "c".repeat(43);

    /** A nanopub whose URI is not a trusty URI, so it gets no artifact code. */
    private static String trig(String npUri) {
        return "<" + npUri + "#Head> {\n" +
                "  <" + npUri + "> a <" + NP_TYPE + "> .\n" +
                "}\n";
    }

    @TempDir
    static Path fixture;

    private static NanopubTestSuite suite;

    @BeforeAll
    static void buildFixture() throws IOException {
        // Both recognized-but-unusual extensions, plus a file that is neither.
        write("valid/plain/plain1.trig", trig("http://example.org/np1"));
        write("valid/plain/README.md", "not a nanopub\n");
        write("valid/signed/signed1.nq",
                "<http://example.org/np2> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + NP_TYPE + "> <http://example.org/np2#Head> .\n");
        write("valid/signed/signed1.xml",
                """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <TriX xmlns="http://www.w3.org/2004/03/trix/trix-1/">
                          <graph>
                            <uri>http://example.org/np3#Head</uri>
                            <triple>
                              <uri>http://example.org/np3</uri>
                              <uri>http://www.w3.org/1999/02/22-rdf-syntax-ns#type</uri>
                              <uri>%s</uri>
                            </triple>
                          </graph>
                        </TriX>
                        """.formatted(NP_TYPE));
        write("valid/trusty/trusty1.trig", trig(TRUSTY_URI));

        // A graph with no nanopub in it, a graph named by a blank node, and a file
        // that doesn't parse at all: three ways to end up with no nanopub URI.
        write("invalid/plain/notype.trig",
                "<http://example.org/g> { <http://example.org/s> <http://example.org/p> <http://example.org/o> . }\n");
        write("invalid/plain/bnodegraph.trig",
                "_:g { <http://example.org/np4> a <" + NP_TYPE + "> . }\n");
        write("invalid/plain/broken.trig", "this is not TriG at all {{{\n");
        // A directory that looks like a nanopub file: listed and indexed like one, but
        // opening it fails, which is the other way extractNanopubUri gives up.
        Files.createDirectories(fixture.resolve("invalid/plain/directory.trig"));
        // The same nanopub as valid/trusty/trusty1.trig, so both lookups return two
        // entries and the category-filtered overloads have to pick between them.
        write("invalid/trusty/trusty1.trig", trig(TRUSTY_URI));

        write("transform/plain/simple1.in.trig", trig("http://example.org/np5"));
        write("transform/plain/orphan.in.trig", trig("http://example.org/np6"));
        for (String key : List.of("rsa-key1", "rsa-key2")) {
            // Only simple1 has a signed counterpart; orphan has none, and is skipped.
            write("transform/signed/" + key + "/simple1.out.code", TRANSFORM_CODE + "\n");
            write("transform/signed/" + key + "/simple1.out.trig", trig(TRANSFORM_URI));
            write("transform/signed/" + key + "/key/id_rsa", "private-key-bytes\n");
            write("transform/signed/" + key + "/key/id_rsa.pub", "public-key-bytes\n");
        }
        // Not a key directory, and must not be treated as one.
        write("transform/signed/notes.txt", "stray file\n");
        write("transform/profile.yaml", "cases: []\n");

        suite = new NanopubTestSuite("fixture-sha", fixture);
    }

    private static void write(String relative, String content) throws IOException {
        Path target = fixture.resolve(relative);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content);
    }

    @Test
    void indexesEveryRecognizedExtensionAndNothingElse() {
        List<String> names = suite.getValid().stream().map(TestSuiteEntry::getName).toList();

        assertEquals(List.of("plain1.trig", "signed1.nq", "signed1.xml", "trusty1.trig"), names);
        assertFalse(names.contains("README.md"), "A non-nanopub file should not be indexed");
    }

    @Test
    void readsTheNanopubUriOutOfEveryRecognizedFormat() {
        assertEquals("http://example.org/np1", uriOf("plain1.trig"));
        assertEquals("http://example.org/np2", uriOf("signed1.nq"));
        assertEquals("http://example.org/np3", uriOf("signed1.xml"));
    }

    @Test
    void filesWithNoReadableNanopubUriAreStillIndexed() {
        for (String name : List.of("notype.trig", "bnodegraph.trig", "broken.trig", "directory.trig")) {
            TestSuiteEntry entry = entry(suite.getInvalid(), name);
            assertNull(entry.getNanopubUri(), name + " should have no nanopub URI");
            assertNull(entry.getArtifactCode(), name + " should have no artifact code");
        }
    }

    @Test
    void aTrustyUriYieldsAnArtifactCode() {
        TestSuiteEntry entry = entry(suite.getValid(), "trusty1.trig");

        assertEquals(TRUSTY_URI, entry.getNanopubUri());
        assertEquals(ArtifactCode.of(TRUSTY_CODE), entry.getArtifactCode());
        assertNull(entry(suite.getValid(), "plain1.trig").getArtifactCode(),
                "A non-trusty URI should yield no artifact code");
    }

    @Test
    void lookupsSpanCategoriesAndCanBeFilteredToOne() {
        ArtifactCode code = ArtifactCode.of(TRUSTY_CODE);

        // The same nanopub is indexed as both a valid and an invalid case.
        assertEquals(2, suite.getByArtifactCode(code).size());
        assertEquals(2, suite.getByNanopubUri(TRUSTY_URI).size());

        assertEquals(TestSuiteSubfolder.TRUSTY,
                suite.getByArtifactCode(code, TestSuiteCategory.VALID).orElseThrow().getSubfolder());
        assertEquals(TestSuiteCategory.INVALID,
                suite.getByArtifactCode(code, TestSuiteCategory.INVALID).orElseThrow().getCategory());
        assertEquals(TestSuiteCategory.INVALID,
                suite.getByNanopubUri(TRUSTY_URI, TestSuiteCategory.INVALID).orElseThrow().getCategory());
    }

    @Test
    void lookupsReturnNothingForUnknownKeys() {
        ArtifactCode unknown = ArtifactCode.of(UNKNOWN_CODE);

        assertTrue(suite.getByArtifactCode(unknown).isEmpty());
        assertTrue(suite.getByNanopubUri("http://example.org/absent").isEmpty());
        assertEquals(Optional.empty(), suite.getByArtifactCode(unknown, TestSuiteCategory.VALID));
        assertEquals(Optional.empty(),
                suite.getByNanopubUri("http://example.org/absent", TestSuiteCategory.VALID));
        // Indexed, but not under this category.
        assertEquals(Optional.empty(),
                suite.getByNanopubUri("http://example.org/np1", TestSuiteCategory.INVALID));
    }

    @Test
    void entriesAreFilteredBySubfolder() {
        assertEquals(1, suite.getValid(TestSuiteSubfolder.PLAIN).size());
        assertEquals(2, suite.getValid(TestSuiteSubfolder.SIGNED).size());
        assertEquals(1, suite.getValid(TestSuiteSubfolder.TRUSTY).size());
        assertEquals(4, suite.getInvalid(TestSuiteSubfolder.PLAIN).size());
        assertEquals(1, suite.getInvalid(TestSuiteSubfolder.TRUSTY).size());
        assertTrue(suite.getInvalid(TestSuiteSubfolder.SIGNED).isEmpty());
    }

    @Test
    void aPlainCaseWithNoSignedCounterpartIsSkipped() {
        assertEquals(Set.of("rsa-key1", "rsa-key2"), Set.copyOf(suite.getKeyNames()));
        assertEquals(2, suite.getTransformCases().size(), "orphan.in.trig has no .out.code");

        TransformTestCase tc = suite.getTransformCases("rsa-key1").getFirst();
        assertEquals("simple1.in.trig", tc.getPlainEntry().getName());
        assertEquals("simple1.out.trig", tc.getSignedEntry().getName());
        assertEquals("rsa-key1", tc.getKeyName());
        assertEquals(TRANSFORM_CODE, tc.getExpectedCode(), "The trailing newline should be stripped");
        assertTrue(suite.getTransformCases("no-such-key").isEmpty());
    }

    @Test
    void entriesExposeTheirFileBothWays() throws IOException {
        TestSuiteEntry entry = entry(suite.getValid(), "plain1.trig");

        assertEquals(fixture.resolve("valid/plain/plain1.trig").toFile(), entry.toFile());
        assertEquals(TestSuiteCategory.VALID, entry.getCategory());
        try (var in = entry.openStream()) {
            assertTrue(new String(in.readAllBytes()).contains("http://example.org/np1"));
        }
    }

    @Test
    void signingKeysArePairedWithTheirKeyDirectory() throws IOException {
        SigningKeyPair key = suite.getSigningKey("rsa-key2");

        assertEquals("rsa-key2", key.getKeyName());
        assertEquals(fixture.resolve("transform/signed/rsa-key2/key/id_rsa").toFile(), key.getPrivateKeyFile());
        assertEquals(fixture.resolve("transform/signed/rsa-key2/key/id_rsa.pub").toFile(), key.getPublicKeyFile());
        try (var priv = key.openPrivateKey(); var pub = key.openPublicKey()) {
            assertEquals("private-key-bytes\n", new String(priv.readAllBytes()));
            assertEquals("public-key-bytes\n", new String(pub.readAllBytes()));
        }
    }

    @Test
    void transformProfileIsResolvedUnderTheRoot() {
        assertEquals(fixture.resolve("transform/profile.yaml").toFile(), suite.getTransformProfile());
    }

    @Test
    void aPinnedVersionIsNotTheLatest() {
        assertEquals("fixture-sha", suite.getVersion());
        assertFalse(suite.isLatest());
    }

    @Test
    void anEmptyTreeIndexesNothingAndHasNoProfile(@TempDir Path empty) {
        NanopubTestSuite bare = new NanopubTestSuite("empty", empty);

        assertTrue(bare.getValid().isEmpty());
        assertTrue(bare.getInvalid().isEmpty());
        assertTrue(bare.getTransformCases().isEmpty());
        assertTrue(bare.getKeyNames().isEmpty());

        IllegalStateException e = assertThrows(IllegalStateException.class, bare::getTransformProfile);
        assertTrue(e.getMessage().contains("empty"), "The message should name the version: " + e.getMessage());
    }

    @Test
    void aCaseDirectoryThatIsNotADirectoryIsReported(@TempDir Path root) throws IOException {
        Files.createDirectories(root.resolve("valid"));
        Files.writeString(root.resolve("valid/plain"), "not a directory\n");

        RuntimeException e = assertThrows(RuntimeException.class, () -> new NanopubTestSuite("broken", root));
        assertTrue(e.getMessage().startsWith("Failed to list: "), e.getMessage());
        assertInstanceOf(IOException.class, e.getCause());
    }

    @Test
    void aSignedRootThatIsNotADirectoryIsReported(@TempDir Path root) throws IOException {
        Files.createDirectories(root.resolve("transform"));
        Files.writeString(root.resolve("transform/signed"), "not a directory\n");

        RuntimeException e = assertThrows(RuntimeException.class, () -> new NanopubTestSuite("broken", root));
        assertTrue(e.getMessage().startsWith("Failed to list signing key directories: "), e.getMessage());
        assertInstanceOf(IOException.class, e.getCause());
    }

    @Test
    void anUnreadableOutCodeIsReported(@TempDir Path root) throws IOException {
        Files.createDirectories(root.resolve("transform/plain"));
        Files.writeString(root.resolve("transform/plain/simple1.in.trig"), trig("http://example.org/np7"));
        // A directory where the code file belongs: it exists, so it is read, and reading it fails.
        Files.createDirectories(root.resolve("transform/signed/rsa-key1/simple1.out.code"));

        RuntimeException e = assertThrows(RuntimeException.class, () -> new NanopubTestSuite("broken", root));
        assertTrue(e.getMessage().startsWith("Failed to read: "), e.getMessage());
        assertInstanceOf(IOException.class, e.getCause());
    }

    @Test
    void extensionsAreMappedToAParserFormat() {
        assertEquals(RDFFormat.TRIG, NanopubTestSuite.resolveFormat("case.trig"));
        assertEquals(RDFFormat.NQUADS, NanopubTestSuite.resolveFormat("case.nq"));
        // .xml is ambiguous — RDF/XML also claims it — so it is pinned to TriX,
        // as is the extension TriX files usually carry.
        assertEquals(RDFFormat.TRIX, NanopubTestSuite.resolveFormat("case.xml"));
        assertEquals(RDFFormat.TRIX, NanopubTestSuite.resolveFormat("case.trix"));
        assertEquals(RDFFormat.TRIG, NanopubTestSuite.resolveFormat("CASE.TRIG"), "The extension is matched case-insensitively");
    }

    @Test
    void anUnpinnedExtensionFallsBackToRio() {
        assertEquals(RDFFormat.TURTLE, NanopubTestSuite.resolveFormat("case.ttl"), "Rio knows this one");
        assertEquals(RDFFormat.TRIG, NanopubTestSuite.resolveFormat("case.unknown"), "And TriG is the default for the rest");
    }

    private static String uriOf(String name) {
        return entry(suite.getValid(), name).getNanopubUri();
    }

    private static TestSuiteEntry entry(List<TestSuiteEntry> entries, String name) {
        return entries.stream()
                .filter(e -> e.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No entry named " + name));
    }

}
