package org.nanopub.testsuite;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents an entry in the test suite, which is a single test case with its associated metadata.
 */
public class TestSuiteEntry {

    private final String name;
    private final Path path;
    private final TestSuiteCategory category;
    private final TestSuiteSubfolder subfolder;
    private final String nanopubUri;
    private final String artifactCode;

    /**
     * Constructor for TestSuiteEntry.
     *
     * @param name         The name of the test case, typically derived from the file name.
     * @param path         The file path to the test case resource.
     * @param category     The category of the test case, such as "valid" or "invalid".
     * @param subfolder    The subfolder within the category, such as "plain", "signed", etc.
     * @param nanopubUri   The URI of the nanopublication.
     * @param artifactCode The artifact code associated with the nanopublication.
     */
    TestSuiteEntry(String name, Path path, TestSuiteCategory category, TestSuiteSubfolder subfolder, String nanopubUri, String artifactCode) {
        this.name = name;
        this.path = path;
        this.category = category;
        this.subfolder = subfolder;
        this.nanopubUri = nanopubUri;
        this.artifactCode = artifactCode;
    }

    /**
     * Gets the name of the test case.
     *
     * @return The name of the test case.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the category of the test case.
     *
     * @return The category of the test case.
     */
    public TestSuiteCategory getCategory() {
        return category;
    }

    /**
     * Gets the subfolder of the test case.
     *
     * @return The subfolder of the test case.
     */
    public TestSuiteSubfolder getSubfolder() {
        return subfolder;
    }

    /**
     * Gets the URI of the nanopublication associated with this test case.
     *
     * @return The URI of the nanopublication.
     */
    public String getNanopubUri() {
        return nanopubUri;
    }

    /**
     * Gets the artifact code associated with the nanopublication for this test case.
     *
     * @return The artifact code of the nanopublication.
     */
    public String getArtifactCode() {
        return artifactCode;
    }

    /**
     * Opens an InputStream to read the content of the test case file.
     *
     * @return An InputStream for the test case file.
     * @throws IOException If an I/O error occurs while opening the stream.
     */
    public InputStream openStream() throws IOException {
        return Files.newInputStream(path);
    }

    /**
     * Converts the Path of the test case file to a File object.
     *
     * @return A File object representing the test case file.
     */
    public File toFile() {
        return path.toFile();
    }

}