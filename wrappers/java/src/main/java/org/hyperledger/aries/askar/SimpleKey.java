package org.hyperledger.aries.askar;

import java.io.Closeable;

/**
 * Simplified high-level wrapper for Askar key operations.
 * Only uses the existing native methods in AskarNative.
 */
public class SimpleKey implements Closeable {
    
    private long handle;
    
    /**
     * Create a SimpleKey wrapper around an existing native handle.
     * Package-private constructor.
     * 
     * @param handle Native key handle
     */
    SimpleKey(long handle) {
        this.handle = handle;
    }
    
    /**
     * Generate a new cryptographic key.
     * 
     * @param algorithm Key algorithm (e.g., "ed25519", "x25519")
     * @param ephemeral Whether the key should be ephemeral
     * @return New SimpleKey instance
     * @throws AskarException if generation fails
     */
    public static SimpleKey generate(String algorithm, boolean ephemeral) throws AskarException {
        long keyHandle = AskarNative.keyGenerate(algorithm, null, ephemeral);
        if (keyHandle == 0) {
            throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Failed to generate key");
        }
        return new SimpleKey(keyHandle);
    }
    
    /**
     * Generate a key using the specified algorithm enum.
     * 
     * @param algorithm Key algorithm enum
     * @param ephemeral Whether the key should be ephemeral
     * @return New SimpleKey instance
     * @throws AskarException if generation fails
     */
    public static SimpleKey generate(KeyAlgorithm algorithm, boolean ephemeral) throws AskarException {
        return generate(algorithm.name().toLowerCase(), ephemeral);
    }
    
    /**
     * Generate a non-ephemeral key.
     * 
     * @param algorithm Key algorithm
     * @return New SimpleKey instance
     * @throws AskarException if generation fails
     */
    public static SimpleKey generate(String algorithm) throws AskarException {
        return generate(algorithm, false);
    }
    
    /**
     * Generate a key from a seed.
     * 
     * @param algorithm Key algorithm
     * @param seed Seed bytes for key generation
     * @param method Seed derivation method
     * @return New SimpleKey instance
     * @throws AskarException if generation fails
     */
    public static SimpleKey fromSeed(String algorithm, byte[] seed, String method) throws AskarException {
        long keyHandle = AskarNative.keyFromSeed(algorithm, seed, method);
        if (keyHandle == 0) {
            throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Failed to generate key from seed");
        }
        return new SimpleKey(keyHandle);
    }
    
    /**
     * Get the algorithm of this key.
     * 
     * @return Algorithm string
     * @throws AskarException if operation fails
     */
    public String getAlgorithm() throws AskarException {
        checkHandle();
        return AskarNative.keyGetAlgorithm(handle);
    }
    
    /**
     * Get the ephemeral flag of this key.
     * Note: This information is not available from current native methods.
     * 
     * @return Always returns false for now
     */
    public boolean isEphemeral() {
        // This method is not available in the current AskarNative implementation
        return false;
    }
    
    /**
     * Get the public key bytes.
     * 
     * @return Public key as byte array
     * @throws AskarException if operation fails
     */
    public byte[] getPublicBytes() throws AskarException {
        checkHandle();
        return AskarNative.keyGetPublicBytes(handle);
    }
    
    /**
     * Get the secret key bytes.
     * 
     * @return Secret key as byte array
     * @throws AskarException if operation fails
     */
    public byte[] getSecretBytes() throws AskarException {
        checkHandle();
        return AskarNative.keyGetSecretBytes(handle);
    }
    
    /**
     * Sign a message using this key.
     * 
     * @param message Message to sign
     * @param sigType Optional signature type
     * @return Signature bytes
     * @throws AskarException if signing fails
     */
    public byte[] signMessage(byte[] message, String sigType) throws AskarException {
        checkHandle();
        return AskarNative.keySignMessage(handle, message, sigType);
    }
    
    /**
     * Sign a message using default signature type.
     * 
     * @param message Message to sign
     * @return Signature bytes
     * @throws AskarException if signing fails
     */
    public byte[] signMessage(byte[] message) throws AskarException {
        return signMessage(message, null);
    }
    
    /**
     * Sign a string message.
     * 
     * @param message Message string to sign
     * @return Signature bytes
     * @throws AskarException if signing fails
     */
    public byte[] signMessage(String message) throws AskarException {
        return signMessage(message.getBytes());
    }
    
    /**
     * Verify a signature against a message.
     * 
     * @param message Original message
     * @param signature Signature to verify
     * @param sigType Optional signature type
     * @return True if signature is valid
     * @throws AskarException if verification fails
     */
    public boolean verifySignature(byte[] message, byte[] signature, String sigType) throws AskarException {
        checkHandle();
        return AskarNative.keyVerifySignature(handle, message, signature, sigType);
    }
    
    /**
     * Verify a signature using default signature type.
     * 
     * @param message Original message
     * @param signature Signature to verify
     * @return True if signature is valid
     * @throws AskarException if verification fails
     */
    public boolean verifySignature(byte[] message, byte[] signature) throws AskarException {
        return verifySignature(message, signature, null);
    }
    
    /**
     * Get the native handle for this key.
     * Package-private for internal use.
     * 
     * @return Native key handle
     */
    long getHandle() {
        return handle;
    }
    
    /**
     * Check if this key is still valid (handle not freed).
     * 
     * @throws AskarException if key is invalid
     */
    private void checkHandle() throws AskarException {
        if (handle == 0) {
            throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Key has been freed");
        }
    }
    
    @Override
    public void close() {
        if (handle != 0) {
            try {
                AskarNative.keyFree(handle);
            } catch (Exception e) {
                // Log error if needed
                System.err.println("Failed to free key: " + e.getMessage());
            } finally {
                handle = 0;
            }
        }
    }
    
    @Override
    public String toString() {
        try {
            if (handle != 0) {
                return String.format("SimpleKey{algorithm='%s', ephemeral=%s, handle=%d}", 
                        getAlgorithm(), isEphemeral(), handle);
            } else {
                return "SimpleKey{freed}";
            }
        } catch (Exception e) {
            return String.format("SimpleKey{handle=%d, error=%s}", handle, e.getMessage());
        }
    }
}