package org.nanopub.testsuite;

public class TransformTestCase {

    private final TestSuiteEntry plainEntry;
    private final TestSuiteEntry signedEntry;
    private final String keyName;
    private final String expectedCode;

    TransformTestCase(TestSuiteEntry plainEntry, TestSuiteEntry signedEntry, String keyName, String expectedCode) {
        this.plainEntry = plainEntry;
        this.signedEntry = signedEntry;
        this.keyName = keyName;
        this.expectedCode = expectedCode;
    }

    public TestSuiteEntry getPlainEntry() {
        return plainEntry;
    }

    public TestSuiteEntry getSignedEntry() {
        return signedEntry;
    }

    public String getKeyName() {
        return keyName;
    }

    public String getExpectedCode() {
        return expectedCode;
    }

}