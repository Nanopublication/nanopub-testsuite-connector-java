package org.nanopub.testsuite;

import net.trustyuri.ArtifactCode;
import net.trustyuri.TrustyUriUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Represents a local copy of the nanopub test suite, which is downloaded on demand from GitHub Test Suite repository.
 */
public class NanopubTestSuite {

    private static final Map<String, NanopubTestSuite> instances = new ConcurrentHashMap<>();
    private final static Logger logger = LoggerFactory.getLogger(NanopubTestSuite.class);

    /**
     * Uses the latest version of the testsuite (main branch).
     */
    public static NanopubTestSuite getLatest() {
        return instances.computeIfAbsent("main", NanopubTestSuite::new);
    }

    /**
     * Uses a specific commit SHA, downloaded once per JVM lifetime.
     */
    public static NanopubTestSuite getAtCommit(String commitSha) {
        if (commitSha == null || commitSha.isBlank()) {
            throw new IllegalArgumentException("Commit SHA must not be blank");
        }
        return instances.computeIfAbsent(commitSha, NanopubTestSuite::new);
    }

    private final String version;
    private final Path root;

    private final List<TestSuiteEntry> valid = new ArrayList<>();
    private final List<TestSuiteEntry> invalid = new ArrayList<>();
    private final List<TransformTestCase> transformCases = new ArrayList<>();
    private final Map<ArtifactCode, List<TestSuiteEntry>> byArtifactCode = new HashMap<>();
    private final Map<String, List<TestSuiteEntry>> byNanopubUri = new HashMap<>();

    private NanopubTestSuite(String version) {
        this.version = version;
        this.root = TestSuiteDownloader.download(version);
        buildIndex();
    }

    private void buildIndex() {
        for (TestSuiteSubfolder sub : TestSuiteSubfolder.values()) {
            loadEntries(root.resolve("valid").resolve(sub.name().toLowerCase()), TestSuiteCategory.VALID, sub, valid);
            loadEntries(root.resolve("invalid").resolve(sub.name().toLowerCase()), TestSuiteCategory.INVALID, sub, invalid);
        }
        loadTransformCases();
    }

    private void loadEntries(Path dir, TestSuiteCategory cat, TestSuiteSubfolder sub, List<TestSuiteEntry> target) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.list(dir)) {
            paths.filter(p -> isNanopubFile(p.getFileName().toString()))
                    .sorted()
                    .forEach(p -> target.add(buildEntry(p, cat, sub)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to list: " + dir, e);
        }
    }

    private void loadTransformCases() {
        List<TestSuiteEntry> plains = new ArrayList<>();
        loadEntries(root.resolve("transform").resolve("plain"), TestSuiteCategory.TRANSFORM, TestSuiteSubfolder.PLAIN, plains);

        Path signedRoot = root.resolve("transform").resolve("signed");
        if (!Files.exists(signedRoot)) {
            return;
        }
        try (Stream<Path> keyDirs = Files.list(signedRoot)) {
            keyDirs.filter(Files::isDirectory).forEach(keyDir -> {
                String keyName = keyDir.getFileName().toString();
                plains.forEach(plain -> {
                    String baseName = plain.getName().replaceFirst("\\.in\\.trig$", "");
                    Path codePath = keyDir.resolve(baseName + ".out.code");
                    Path trigPath = keyDir.resolve(baseName + ".out.trig");
                    String code = readFile(codePath);
                    if (code == null) {
                        return;
                    }
                    TestSuiteEntry signedEntry = buildEntry(trigPath, TestSuiteCategory.TRANSFORM, TestSuiteSubfolder.SIGNED);
                    transformCases.add(new TransformTestCase(plain, signedEntry, keyName, code.strip()));
                });
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to list signing key directories: " + signedRoot, e);
        }
    }

    private TestSuiteEntry buildEntry(Path path, TestSuiteCategory cat, TestSuiteSubfolder sub) {
        String name = path.getFileName().toString();
        String nanopubUri = extractNanopubUri(path);
        ArtifactCode artifactCode = TrustyUriUtils.isPotentialTrustyUri(nanopubUri) ? ArtifactCode.of(TrustyUriUtils.getArtifactCode(nanopubUri)) : null;
        logger.info("Indexing entry: " + name + " | category: " + cat + " | subfolder: " + sub + " | nanopub URI: " + nanopubUri + " | artifact code: " + artifactCode);
        TestSuiteEntry entry = new TestSuiteEntry(name, path, cat, sub, nanopubUri, artifactCode);
        if (nanopubUri != null) {
            byNanopubUri.computeIfAbsent(nanopubUri, k -> new ArrayList<>()).add(entry);
        }
        if (artifactCode != null) {
            byArtifactCode.computeIfAbsent(artifactCode, k -> new ArrayList<>()).add(entry);
        }
        return entry;
    }

    /**
     * All valid entries across all subfolders.
     */
    public List<TestSuiteEntry> getValid() {
        return Collections.unmodifiableList(valid);
    }

    /**
     * Valid entries in a specific subfolder (PLAIN, SIGNED, TRUSTY).
     */
    public List<TestSuiteEntry> getValid(TestSuiteSubfolder subfolder) {
        return valid.stream().filter(e -> e.getSubfolder() == subfolder).toList();
    }

    /**
     * All invalid entries across all subfolders.
     */
    public List<TestSuiteEntry> getInvalid() {
        return Collections.unmodifiableList(invalid);
    }

    /**
     * Invalid entries in a specific subfolder.
     */
    public List<TestSuiteEntry> getInvalid(TestSuiteSubfolder subfolder) {
        return invalid.stream().filter(e -> e.getSubfolder() == subfolder).toList();
    }

    /**
     * All transform test cases across both signing keys.
     */
    public List<TransformTestCase> getTransformCases() {
        return Collections.unmodifiableList(transformCases);
    }

    /**
     * Transform test cases for a specific signing key, e.g. "rsa-key1".
     */
    public List<TransformTestCase> getTransformCases(String keyName) {
        return transformCases.stream().filter(tc -> tc.getKeyName().equals(keyName)).toList();
    }

    /**
     * Look up any indexed entry by its artifact code.
     */
    public List<TestSuiteEntry> getByArtifactCode(ArtifactCode artifactCode) {
        return Collections.unmodifiableList(byArtifactCode.getOrDefault(artifactCode, Collections.emptyList()));
    }

    public Optional<TestSuiteEntry> getByArtifactCode(ArtifactCode artifactCode, TestSuiteCategory category) {
        return getByArtifactCode(artifactCode).stream()
                .filter(e -> e.getCategory() == category)
                .findFirst();
    }

    /**
     * Look up all indexed entries sharing a nanopub URI (may span multiple categories).
     */
    public List<TestSuiteEntry> getByNanopubUri(String nanopubUri) {
        return Collections.unmodifiableList(byNanopubUri.getOrDefault(nanopubUri, Collections.emptyList()));
    }

    /**
     * Look up a single entry by nanopub URI, filtered to a specific category.
     * Returns empty if no match exists for that URI + category combination.
     */
    public Optional<TestSuiteEntry> getByNanopubUri(String nanopubUri, TestSuiteCategory category) {
        return getByNanopubUri(nanopubUri).stream()
                .filter(e -> e.getCategory() == category)
                .findFirst();
    }

    /**
     * Available signing key names, e.g. ["rsa-key1", "rsa-key2"].
     */
    public List<String> getKeyNames() {
        return transformCases.stream().map(TransformTestCase::getKeyName).distinct().toList();
    }

    /**
     * Access the signing key pair for a given key name.
     */
    public SigningKeyPair getSigningKey(String keyName) {
        Path keyDir = root.resolve("transform").resolve("signed").resolve(keyName).resolve("key");
        return new SigningKeyPair(keyName, keyDir.resolve("id_rsa"), keyDir.resolve("id_rsa.pub"));
    }

    /**
     * Returns the version string this instance was created with.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns true if this instance is tracking main rather than a pinned commit.
     */
    public boolean isLatest() {
        return version.equals("main");
    }

    private static String extractNanopubUri(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            Model model = new LinkedHashModel();
            RDFFormat format = Rio.getParserFormatForFileName(path.getFileName().toString()).orElse(RDFFormat.TRIG);
            RDFParser parser = Rio.createParser(format);
            parser.setRDFHandler(new StatementCollector(model));
            parser.parse(in, path.toUri().toString());

            IRI candidateUri = Values.iri("http://www.nanopub.org/nschema#Nanopublication");
            for (Resource ctx : model.contexts()) {
                if (!(ctx instanceof IRI)) {
                    continue;
                }
                for (Statement st : model.filter(null, RDF.TYPE, candidateUri, ctx)) {
                    Resource subj = st.getSubject();
                    if (subj != null) {
                        return subj.stringValue();
                    }
                }
            }
            return null;
        } catch (IOException | RDFParseException | RDFHandlerException e) {
            return null;
        }
    }

    private static String readFile(Path path) {
        if (!Files.exists(path)) {
            return null;
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read: " + path, e);
        }
    }

    private static boolean isNanopubFile(String name) {
        return name.endsWith(".trig") || name.endsWith(".nq") || name.endsWith(".xml");
    }

    /**
     * Returns the transform profile YAML file, which contains the expected RDF transformations for each test case.
     *
     * @return the transform profile file
     */
    public File getTransformProfile() {
        Path profile = root.resolve("transform").resolve("profile.yaml");
        if (!Files.exists(profile)) {
            throw new IllegalStateException("profile.yaml not found in testsuite @ " + version);
        }
        return profile.toFile();
    }

}