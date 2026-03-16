package org.nanopub.testsuite;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents a signing key pair (private and public key) for testing digital signatures of nanopublications.
 */
public class SigningKeyPair {

    private final String keyName;
    private final Path privateKeyPath;
    private final Path publicKeyPath;

    /**
     * Creates a signing key pair with the given name and paths to the private and public key files.
     *
     * @param keyName        a name for the key pair, e.g. "rsa-key-1" or "ecdsa-key-1"
     * @param privateKeyPath the path to the private key file
     * @param publicKeyPath  the path to the public key file
     */
    SigningKeyPair(String keyName, Path privateKeyPath, Path publicKeyPath) {
        this.keyName = keyName;
        this.privateKeyPath = privateKeyPath;
        this.publicKeyPath = publicKeyPath;
    }

    /**
     * Returns the name of this key pair, which is used in the test suite to identify it.
     *
     * @return the key name
     */
    public String getKeyName() {
        return keyName;
    }

    /**
     * Opens an input stream to the private key file. The caller is responsible for closing the stream after use.
     *
     * @return an input stream to the private key file
     * @throws IOException if the private key file cannot be opened
     */
    public InputStream openPrivateKey() throws IOException {
        return Files.newInputStream(privateKeyPath);
    }

    /**
     * Opens an input stream to the public key file. The caller is responsible for closing the stream after use.
     *
     * @return an input stream to the public key file
     * @throws IOException if the public key file cannot be opened
     */
    public InputStream openPublicKey() throws IOException {
        return Files.newInputStream(publicKeyPath);
    }

    /**
     * Returns the private key file as a File object. This can be used for APIs that require a File reference.
     *
     * @return the private key file
     */
    public File getPrivateKeyFile() {
        return privateKeyPath.toFile();
    }

    /**
     * Returns the public key file as a File object. This can be used for APIs that require a File reference.
     *
     * @return the public key file
     */
    public File getPublicKeyFile() {
        return publicKeyPath.toFile();
    }

}