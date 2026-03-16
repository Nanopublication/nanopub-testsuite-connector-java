package org.nanopub.testsuite;

/**
 * A test case for testing the transformation of a plain nanopublication to a signed nanopublication.
 */
public class TransformTestCase {

    private final TestSuiteEntry plainEntry;
    private final TestSuiteEntry signedEntry;
    private final String keyName;
    private final String expectedCode;

    /**
     * Creates a new test case.
     *
     * @param plainEntry   the test suite entry for the plain nanopublication
     * @param signedEntry  the test suite entry for the signed nanopublication
     * @param keyName      the name of the key to be used for signing
     * @param expectedCode the expected result code of the transformation
     */
    TransformTestCase(TestSuiteEntry plainEntry, TestSuiteEntry signedEntry, String keyName, String expectedCode) {
        this.plainEntry = plainEntry;
        this.signedEntry = signedEntry;
        this.keyName = keyName;
        this.expectedCode = expectedCode;
    }

    /**
     * Gets the test suite entry for the plain nanopublication.
     *
     * @return the test suite entry for the plain nanopublication
     */
    public TestSuiteEntry getPlainEntry() {
        return plainEntry;
    }

    /**
     * Gets the test suite entry for the signed nanopublication.
     *
     * @return the test suite entry for the signed nanopublication
     */
    public TestSuiteEntry getSignedEntry() {
        return signedEntry;
    }

    /**
     * Gets the name of the key to be used for signing.
     *
     * @return the name of the key to be used for signing
     */
    public String getKeyName() {
        return keyName;
    }

    /**
     * Gets the expected result code of the transformation.
     *
     * @return the expected result code of the transformation
     */
    public String getExpectedCode() {
        return expectedCode;
    }

}