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
validPlains.forEach(e -> System.out.println(e.getName() + " -> " + e.getPath()));
```

Load a given version of the test suite version using the commit SHA:

```java
NanopubTestSuite atCommit = NanopubTestSuite.getAtCommit("a1b2c3d...");
System.out.println("Loaded testsuite version: " + atCommit.getVersion());
```

Access nanopublication entries by artifact code or full URI:

```java
NanopubTestSuite suite = NanopubTestSuite.getLatest();
TestSuiteEntry entryByCode = suite.getByArtifactCode("RA1sViVmXf-W2aZW4Qk74KTaiD9gpLBPe2LhMsinHKKz8");
System.out.println("Entry for artifact code 'RA1sViVmXf-W2aZW4Qk74KTaiD9gpLBPe2LhMsinHKKz8': " + entryByCode.getName());

TestSuiteEntry entryByUri = suite.getByNanopubUri("http://purl.org/np/RAPPdsJKoVVp7KZTjdS3D2MvxfkNa-G4JDrnLjeMQFwnY");
System.out.println("Entry for URI 'http://purl.org/np/RAPPdsJKoVVp7KZTjdS3D2MvxfkNa-G4JDrnLjeMQFwnY': " + entryByUri.getName());
```

Access transform cases for a named signing key:

```java
suite.getTransformCases("rsa-key1").forEach(tc ->
    System.out.println(tc.getPlain().getName() + " -> " + tc.getSigned().getName())
);
```

## API overview

- `NanopubTestSuite.getLatest()` — returns an instance tracking the `main` branch.
- `NanopubTestSuite.getAtCommit(String commitSha)` — return an instance for the specified commit SHA.
- `getValid()`, `getInvalid()` — all valid/invalid `TestSuiteEntry` instances.
- `getValid(TestSuiteSubfolder)`, `getInvalid(TestSuiteSubfolder)` — filter by subfolder (`PLAIN`, `SIGNED`, `TRUSTY`).
- `getTransformCases()` — all `TransformTestCase` instances.
- `getTransformCases(String keyName)` — transform cases for a given signing key (e.g. `"rsa-key1"`).
- `getSigningKey(String keyName)` — returns a `SigningKeyPair` (paths to private/public key files).
- `getByArtifactCode(String)` — lookup by Trusty URI artifact code.
- `getByNanopubUri(String)` — lookup by full nanopub URI.

## Notes & troubleshooting

- The connector downloads GitHub tarballs (`archive/<ref>.tar.gz`). An internet connection is required when fetching a new version/commit.
- Downloaded data is extracted into a temporary directory. The implementation deletes the downloaded tarball after extraction, but the extracted files remain in a temp dir until JVM exit (temp dir created with `deleteOnExit()`).
- If extraction fails, ensure your environment allows outgoing HTTPS and has write access to the system temporary directory.
