package org.nanopub.testsuite;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class SigningKeyPair {

    private final String keyName;
    private final Path privateKeyPath;
    private final Path publicKeyPath;

    SigningKeyPair(String keyName, Path privateKeyPath, Path publicKeyPath) {
        this.keyName = keyName;
        this.privateKeyPath = privateKeyPath;
        this.publicKeyPath = publicKeyPath;
    }

    public String getKeyName() {
        return keyName;
    }

    public InputStream openPrivateKey() throws IOException {
        return Files.newInputStream(privateKeyPath);
    }

    public InputStream openPublicKey() throws IOException {
        return Files.newInputStream(publicKeyPath);
    }

    public File getPrivateKeyFile() {
        return privateKeyPath.toFile();
    }

    public File getPublicKeyFile() {
        return publicKeyPath.toFile();
    }

}