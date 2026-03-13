package org.nanopub.testsuite;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestSuiteEntry {

    private final String name;
    private final Path path;
    private final TestSuiteCategory category;
    private final TestSuiteSubfolder subfolder;
    private final String nanopubUri;
    private final String artifactCode;

    TestSuiteEntry(String name, Path path, TestSuiteCategory category, TestSuiteSubfolder subfolder, String nanopubUri, String artifactCode) {
        this.name = name;
        this.path = path;
        this.category = category;
        this.subfolder = subfolder;
        this.nanopubUri = nanopubUri;
        this.artifactCode = artifactCode;
    }

    public String getName() {
        return name;
    }

    public TestSuiteCategory getCategory() {
        return category;
    }

    public TestSuiteSubfolder getSubfolder() {
        return subfolder;
    }

    public String getNanopubUri() {
        return nanopubUri;
    }

    public String getArtifactCode() {
        return artifactCode;
    }

    public InputStream openStream() throws IOException {
        return Files.newInputStream(path);
    }

    public File toFile() {
        return path.toFile();
    }

}