package org.hyperledger.aries.askar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;

import java.util.Map;

/**
 * Represents a cryptographic key or keypair.
 */
public class Key implements AutoCloseable {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private long handle;
    private boolean closed = false;

    Key(long handle) {
        this.handle = handle;
        LibraryLoader.ensureInitialized();
    }

    /**
     * Generate a new key.
     * @param algorithm The key algorithm
     * @param ephemeral Whether the key should be ephemeral
     * @return A new Key instance
     * @throws AskarException If generation fails
     */
    public static Key generate(KeyAlgorithm algorithm, boolean ephemeral) throws AskarException {
        return generate(algorithm, null, ephemeral);
    }

    /**
     * Generate a new key with specified backend.
     * @param algorithm The key algorithm
     * @param backend The key backend (optional)
     * @param ephemeral Whether the key should be ephemeral
     * @return A new Key instance
     * @throws AskarException If generation fails
     */
    public static Key generate(KeyAlgorithm algorithm, String backend, boolean ephemeral) throws AskarException {
        LibraryLoader.ensureInitialized();
        PointerByReference keyHandle = new PointerByReference();

        int result = AskarLibrary.INSTANCE.askar_key_generate(
                algorithm.getAlgorithmName(),
                backend,
                (byte) (ephemeral ? 1 : 0),
                keyHandle
        );

        LibraryLoader.checkError(result);
        return new Key(Pointer.nativeValue(keyHandle.getValue()));
    }

    /**
     * Create a key from a seed.
     * @param algorithm The key algorithm
     * @param seed The seed bytes
     * @param method The seed derivation method
     * @return A new Key instance
     * @throws AskarException If creation fails
     */
    public static Key fromSeed(KeyAlgorithm algorithm, byte[] seed, SeedMethod method) throws AskarException {
        LibraryLoader.ensureInitialized();
        PointerByReference keyHandle = new PointerByReference();

        AskarLibrary.RawBuffer seedBuffer = LibraryLoader.bytesToRawBuffer(seed);
        try {
            int result = AskarLibrary.INSTANCE.askar_key_from_seed(
                    algorithm.getAlgorithmName(),
                    seedBuffer,
                    method != null ? method.getMethodName() : null,
                    keyHandle
            );

            LibraryLoader.checkError(result);
            return new Key(Pointer.nativeValue(keyHandle.getValue()));
        } finally {
            // seedBuffer is Java-allocated, no need to free
        }
    }

    /**
     * Create a key from secret bytes.
     * @param algorithm The key algorithm
     * @param secretBytes The secret key bytes
     * @return A new Key instance
     * @throws AskarException If creation fails
     */
    public static Key fromSecretBytes(KeyAlgorithm algorithm, byte[] secretBytes) throws AskarException {
        LibraryLoader.ensureInitialized();
        PointerByReference keyHandle = new PointerByReference();

        AskarLibrary.RawBuffer secretBuffer = LibraryLoader.bytesToRawBuffer(secretBytes);
        try {
            int result = AskarLibrary.INSTANCE.askar_key_from_secret_bytes(
                    algorithm.getAlgorithmName(),
                    secretBuffer,
                    keyHandle
            );

            LibraryLoader.checkError(result);
            return new Key(Pointer.nativeValue(keyHandle.getValue()));
        } finally {
            // secretBuffer is Java-allocated, no need to free
        }
    }

    /**
     * Create a key from public bytes.
     * @param algorithm The key algorithm
     * @param publicBytes The public key bytes
     * @return A new Key instance
     * @throws AskarException If creation fails
     */
    public static Key fromPublicBytes(KeyAlgorithm algorithm, byte[] publicBytes) throws AskarException {
        LibraryLoader.ensureInitialized();
        PointerByReference keyHandle = new PointerByReference();

        AskarLibrary.RawBuffer publicBuffer = LibraryLoader.bytesToRawBuffer(publicBytes);
        try {
            int result = AskarLibrary.INSTANCE.askar_key_from_public_bytes(
                    algorithm.getAlgorithmName(),
                    publicBuffer,
                    keyHandle
            );

            LibraryLoader.checkError(result);
            return new Key(Pointer.nativeValue(keyHandle.getValue()));
        } finally {
            // publicBuffer is Java-allocated, no need to free
        }
    }

    /**
     * Create a key from JWK.
     * @param jwk The JWK as a Map or JSON string
     * @return A new Key instance
     * @throws AskarException If creation fails
     */
    public static Key fromJwk(Object jwk) throws AskarException {
        LibraryLoader.ensureInitialized();
        PointerByReference keyHandle = new PointerByReference();

        byte[] jwkBytes;
        if (jwk instanceof String) {
            jwkBytes = ((String) jwk).getBytes();
        } else if (jwk instanceof Map) {
            try {
                jwkBytes = objectMapper.writeValueAsBytes(jwk);
            } catch (JsonProcessingException e) {
                throw new AskarException(AskarException.ErrorCode.INPUT, "Failed to serialize JWK", e);
            }
        } else {
            throw new AskarException(AskarException.ErrorCode.INPUT, "JWK must be a String or Map");
        }

        AskarLibrary.RawBuffer jwkBuffer = LibraryLoader.bytesToRawBuffer(jwkBytes);
        try {
            int result = AskarLibrary.INSTANCE.askar_key_from_jwk(jwkBuffer, keyHandle);
            LibraryLoader.checkError(result);
            return new Key(Pointer.nativeValue(keyHandle.getValue()));
        } finally {
            LibraryLoader.freeBuffer(jwkBuffer);
        }
    }

    /**
     * Get the algorithm of this key.
     * @return The key algorithm
     * @throws AskarException If operation fails
     */
    public KeyAlgorithm getAlgorithm() throws AskarException {
        checkClosed();
        PointerByReference algorithmRef = new PointerByReference();

        int result = AskarLibrary.INSTANCE.askar_key_get_algorithm(handle, algorithmRef);
        LibraryLoader.checkError(result);

        try {
            String algorithmStr = algorithmRef.getValue().getString(0);
            return KeyAlgorithm.fromString(algorithmStr);
        } finally {
            LibraryLoader.freeString(algorithmRef.getValue());
        }
    }

    /**
     * Check if this key is ephemeral.
     * @return true if the key is ephemeral
     * @throws AskarException If operation fails
     */
    public boolean isEphemeral() throws AskarException {
        checkClosed();
        PointerByReference ephemeralRef = new PointerByReference();

        int result = AskarLibrary.INSTANCE.askar_key_get_ephemeral(handle, ephemeralRef);
        LibraryLoader.checkError(result);

        return ephemeralRef.getValue().getByte(0) != 0;
    }

    /**
     * Get the public key bytes.
     * @return The public key bytes
     * @throws AskarException If operation fails
     */
    public byte[] getPublicBytes() throws AskarException {
        checkClosed();
        PointerByReference publicBytesRef = new PointerByReference();

        int result = AskarLibrary.INSTANCE.askar_key_get_public_bytes(handle, publicBytesRef);
        LibraryLoader.checkError(result);

        try {
            // Read the OutputBuffer structure from the pointer
            AskarLibrary.OutputBuffer buffer = new AskarLibrary.OutputBuffer(publicBytesRef.getValue());
            return buffer.toByteArray();
        } finally {
            // Free the Rust-allocated buffer
            AskarLibrary.OutputBuffer bufferToFree = new AskarLibrary.OutputBuffer(publicBytesRef.getValue());
            LibraryLoader.freeOutputBuffer(bufferToFree);
        }
    }

    /**
     * Get the secret key bytes.
     * @return The secret key bytes
     * @throws AskarException If operation fails
     */
    public byte[] getSecretBytes() throws AskarException {
        checkClosed();
        PointerByReference secretBytesRef = new PointerByReference();

        int result = AskarLibrary.INSTANCE.askar_key_get_secret_bytes(handle, secretBytesRef);
        LibraryLoader.checkError(result);

        try {
            // Read the OutputBuffer structure from the pointer
            AskarLibrary.OutputBuffer buffer = new AskarLibrary.OutputBuffer(secretBytesRef.getValue());
            return buffer.toByteArray();
        } finally {
            // Free the Rust-allocated buffer
            AskarLibrary.OutputBuffer bufferToFree = new AskarLibrary.OutputBuffer(secretBytesRef.getValue());
            LibraryLoader.freeOutputBuffer(bufferToFree);
        }
    }

    /**
     * Get the public key as JWK.
     * @param algorithm The algorithm to use for the JWK (optional)
     * @return The JWK as a JSON string
     * @throws AskarException If operation fails
     */
    public String getJwkPublic(KeyAlgorithm algorithm) throws AskarException {
        checkClosed();
        PointerByReference jwkRef = new PointerByReference();

        int result = AskarLibrary.INSTANCE.askar_key_get_jwk_public(
                handle,
                algorithm != null ? algorithm.getAlgorithmName() : null,
                jwkRef
        );
        LibraryLoader.checkError(result);

        try {
            return jwkRef.getValue().getString(0);
        } finally {
            LibraryLoader.freeString(jwkRef.getValue());
        }
    }

    /**
     * Get the secret key as JWK.
     * @return The JWK bytes
     * @throws AskarException If operation fails
     */
    public byte[] getJwkSecret() throws AskarException {
        checkClosed();
        PointerByReference jwkRef = new PointerByReference();

        int result = AskarLibrary.INSTANCE.askar_key_get_jwk_secret(handle, jwkRef);
        LibraryLoader.checkError(result);

        try {
            // Read the RawBuffer structure from the pointer
            AskarLibrary.RawBuffer buffer = Structure.newInstance(AskarLibrary.RawBuffer.class, jwkRef.getValue());
            buffer.read();
            return buffer.toByteArray();
        } finally {
            LibraryLoader.freeBuffer(Structure.newInstance(AskarLibrary.RawBuffer.class, jwkRef.getValue()));
        }
    }

    /**
     * Sign a message.
     * @param message The message to sign
     * @param signatureType The signature type (optional)
     * @return The signature bytes
     * @throws AskarException If operation fails
     */
    public byte[] signMessage(byte[] message, String signatureType) throws AskarException {
        checkClosed();
        PointerByReference signatureRef = new PointerByReference();

        AskarLibrary.RawBuffer messageBuffer = LibraryLoader.bytesToRawBuffer(message);
        try {
            int result = AskarLibrary.INSTANCE.askar_key_sign_message(
                    handle,
                    messageBuffer,
                    signatureType,
                    signatureRef
            );
            LibraryLoader.checkError(result);

            // Read the OutputBuffer structure from the pointer
            AskarLibrary.OutputBuffer buffer = new AskarLibrary.OutputBuffer(signatureRef.getValue());
            byte[] signature = buffer.toByteArray();
            
            // Free the Rust-allocated buffer
            LibraryLoader.freeOutputBuffer(buffer);
            
            return signature;
        } finally {
            // messageBuffer is Java-allocated, no need to free
        }
    }

    /**
     * Verify a signature.
     * @param message The original message
     * @param signature The signature to verify
     * @param signatureType The signature type (optional)
     * @return true if the signature is valid
     * @throws AskarException If operation fails
     */
    public boolean verifySignature(byte[] message, byte[] signature, String signatureType) throws AskarException {
        checkClosed();
        PointerByReference validRef = new PointerByReference();

        AskarLibrary.RawBuffer messageBuffer = LibraryLoader.bytesToRawBuffer(message);
        AskarLibrary.RawBuffer signatureBuffer = LibraryLoader.bytesToRawBuffer(signature);
        try {
            int result = AskarLibrary.INSTANCE.askar_key_verify_signature(
                    handle,
                    messageBuffer,
                    signatureBuffer,
                    signatureType,
                    validRef
            );
            LibraryLoader.checkError(result);

            return validRef.getValue().getByte(0) != 0;
        } finally {
            LibraryLoader.freeBuffer(messageBuffer);
            LibraryLoader.freeBuffer(signatureBuffer);
        }
    }

    /**
     * Convert this key to another algorithm.
     * @param algorithm The target algorithm
     * @return A new Key instance with the converted algorithm
     * @throws AskarException If conversion fails
     */
    public Key convert(KeyAlgorithm algorithm) throws AskarException {
        checkClosed();
        PointerByReference convertedKeyRef = new PointerByReference();

        int result = AskarLibrary.INSTANCE.askar_key_convert(
                handle,
                algorithm.getAlgorithmName(),
                convertedKeyRef
        );
        LibraryLoader.checkError(result);

        return new Key(Pointer.nativeValue(convertedKeyRef.getValue()));
    }

    long getHandle() {
        return handle;
    }

    private void checkClosed() throws AskarException {
        if (closed) {
            throw new AskarException(AskarException.ErrorCode.WRAPPER, "Key has been closed");
        }
    }

    @Override
    public void close() {
        if (!closed && handle != 0) {
            AskarLibrary.INSTANCE.askar_key_free(handle);
            handle = 0;
            closed = true;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}