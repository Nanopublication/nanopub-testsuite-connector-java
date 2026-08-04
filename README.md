[![Maven Central Version](https://img.shields.io/maven-central/v/org.nanopub/nanopub-testsuite-connector)](https://mvnrepository.com/artifact/org.nanopub/nanopub-testsuite-connector)
[![Tests](https://github.com/Nanopublication/nanopub-testsuite-connector-java/actions/workflows/maven-test.yml/badge.svg)](https://github.com/Nanopublication/nanopub-testsuite-connector-java/actions/workflows/maven-test.yml)
[![Coverage Status](https://coveralls.io/repos/github/Nanopublication/nanopub-testsuite-connector-java/badge.svg?branch=master)](https://coveralls.io/github/Nanopublication/nanopub-testsuite-connector-java?branch=master)
[![semantic-release: angular](https://img.shields.io/badge/semantic--release-angular-e10079?logo=semantic-release)](https://github.com/semantic-release/semantic-release)

# Nanopublication Test Suite Connector

A Java connector that downloads and exposes the [Nanopublication Test Suite (nanopub-testsuite)](https://github.com/nanopublication/nanopub-testsuite) contents for programmatic use.

This project provides a lightweight API to fetch the official Nanopub Test Suite from GitHub (as a `.tar.gz` archive), extract it locally, and index test cases so other code can easily consume valid/invalid test nanopublications, transform test cases, and signing keys.

## Table of contents

- [Usage](#usage)
- [Quick examples](#quick-examples)
- [API overview](#api-overview)
- [Notes & troubleshooting](#notes--troubleshooting)

## Usage

Add the connector to your project (Maven):

```xml
<dependency>
  <groupId>org.nanopub</groupId>
  <artifactId>nanopub-testsuite-connector</artifactId>
  <version>1.0.0</version>
  <scope>test</scope>
</dependency>
```

## Quick examples

Basic example: load the **latest** testsuite and list all valid PLAIN entries.

```java
import org.nanopub.testsuite.NanopubTestSuite;
import org.nanopub.testsuite.TestSuiteEntry;
import org.nanopub.testsuite.TestSuiteSubfolder;

NanopubTestSuite suite = NanopubTestSuite.getLatest();
List<TestSuiteEntry> validPlains = suite.getValid(TestSuiteSubfolder.PLAIN);
validPlains.forEach(e -> System.out.println(e.getName() + " -> " + e.toFile()));
```

Load a given version of the test suite version using the commit SHA:

```java
NanopubTestSuite atCommit = NanopubTestSuite.getAtCommit("a1b2c3d...");
System.out.println("Loaded testsuite version: " + atCommit.getVersion());
```

Access nanopublication entries by artifact code or full URI. Note that a single
artifact code or URI can map to more than one entry (e.g. the same nanopub may
appear as both a valid and an invalid case), so the lookups return a list. Use
the category-filtered overloads when you want a single entry.

```java
import net.trustyuri.ArtifactCode;

NanopubTestSuite suite = NanopubTestSuite.getLatest();

ArtifactCode code = ArtifactCode.of("RAPpJU5UOB4pavfWyk7FE3WQiam5yBpmIlviAQWtBSC4M");
List<TestSuiteEntry> entriesByCode = suite.getByArtifactCode(code);
entriesByCode.forEach(e -> System.out.println("Entry for artifact code: " + e.getName()));

// Or narrow to a single entry in a given category:
Optional<TestSuiteEntry> validByCode = suite.getByArtifactCode(code, TestSuiteCategory.VALID);

List<TestSuiteEntry> entriesByUri = suite.getByNanopubUri(
        "http://example.org/nanopub-validator-example/RAPpJU5UOB4pavfWyk7FE3WQiam5yBpmIlviAQWtBSC4M");
entriesByUri.forEach(e -> System.out.println("Entry for URI: " + e.getName()));
```

Access transform cases for a named signing key:

```java
suite.getTransformCases("rsa-key1").forEach(tc ->
    System.out.println(tc.getPlainEntry().getName() + " -> " + tc.getSignedEntry().getName())
);
```

## API overview

- `NanopubTestSuite.getLatest()` — returns an instance tracking the `main` branch.
- `NanopubTestSuite.getAtCommit(String commitSha)` — return an instance for the specified commit SHA.
- `getValid()`, `getInvalid()` — all valid/invalid `TestSuiteEntry` instances.
- `getValid(TestSuiteSubfolder)`, `getInvalid(TestSuiteSubfolder)` — filter by subfolder (`PLAIN`, `SIGNED`, `TRUSTY`).
- `getTransformCases()` — all `TransformTestCase` instances.
- `getTransformCases(String keyName)` — transform cases for a given signing key (e.g. `"rsa-key1"`).
- `getKeyNames()` — the distinct signing key names available (e.g. `["rsa-key1", "rsa-key2"]`).
- `getSigningKey(String keyName)` — returns a `SigningKeyPair` exposing streams (`openPrivateKey()`, `openPublicKey()`) and `File` references (`getPrivateKeyFile()`, `getPublicKeyFile()`) to the key files.
- `getByArtifactCode(ArtifactCode)` — all entries for a Trusty URI artifact code (a `List`, since one code may appear in multiple categories).
- `getByArtifactCode(ArtifactCode, TestSuiteCategory)` — the first matching entry in that category, as an `Optional`.
- `getByNanopubUri(String)` — all entries sharing a full nanopub URI (a `List`).
- `getByNanopubUri(String, TestSuiteCategory)` — the first matching entry in that category, as an `Optional`.
- `getTransformProfile()` — the transform `profile.yaml` `File` describing the expected transformations.
- `getVersion()` / `isLatest()` — the version string this instance tracks, and whether it is `main` rather than a pinned commit.

## Notes & troubleshooting

- The connector downloads GitHub tarballs (`archive/<ref>.tar.gz`). An internet connection is required when fetching a new version/commit.
- Downloaded data is extracted into a temporary directory. The implementation deletes the downloaded tarball right after extraction, and removes the extracted directory recursively on JVM exit via a shutdown hook. The extracted files stay available for the entire JVM lifetime, so entries can be used freely across a test run.
- If extraction fails, ensure your environment allows outgoing HTTPS and has write access to the system temporary directory.
