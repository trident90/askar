package org.hyperledger.aries.askar;

/**
 * Supported key algorithms in Askar.
 */
public enum KeyAlgorithm {
    // ECDSA algorithms
    ED25519("ed25519"),
    ED448("ed448"),
    ES256("es256"),
    ES256K("es256k"),
    ES384("es384"),
    ES512("es512"),

    // ECDH algorithms
    X25519("x25519"),
    X448("x448"),

    // ChaCha20Poly1305 AEAD
    CHACHA20_POLY1305("chacha20-poly1305"),

    // AES-GCM AEAD
    AES128_GCM("aes128-gcm"),
    AES256_GCM("aes256-gcm"),

    // Key wrapping
    AES128_KW("aes128-kw"),
    AES256_KW("aes256-kw"),

    // BLS signatures
    BLS12_381_G1("bls12_381-g1"),
    BLS12_381_G2("bls12_381-g2"),
    BLS12_381_G1G2("bls12_381-g1g2"),

    // RSA
    RSA("rsa");

    private final String algorithmName;

    KeyAlgorithm(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    /**
     * Get the algorithm name as used by the native library.
     * @return The algorithm name
     */
    public String getAlgorithmName() {
        return algorithmName;
    }

    /**
     * Parse algorithm from string.
     * @param algorithm The algorithm string
     * @return The KeyAlgorithm enum value
     * @throws IllegalArgumentException if algorithm is not supported
     */
    public static KeyAlgorithm fromString(String algorithm) {
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null");
        }

        for (KeyAlgorithm alg : values()) {
            if (alg.algorithmName.equalsIgnoreCase(algorithm)) {
                return alg;
            }
        }

        throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
    }

    /**
     * Check if this is an ECDSA signing algorithm.
     * @return true if this is an ECDSA algorithm
     */
    public boolean isEcdsa() {
        return this == ED25519 || this == ED448 || this == ES256 || this == ES256K || this == ES384 || this == ES512;
    }

    /**
     * Check if this is an ECDH key exchange algorithm.
     * @return true if this is an ECDH algorithm
     */
    public boolean isEcdh() {
        return this == X25519 || this == X448;
    }

    /**
     * Check if this is an AEAD encryption algorithm.
     * @return true if this is an AEAD algorithm
     */
    public boolean isAead() {
        return this == CHACHA20_POLY1305 || this == AES128_GCM || this == AES256_GCM;
    }

    /**
     * Check if this is a key wrapping algorithm.
     * @return true if this is a key wrapping algorithm
     */
    public boolean isKeyWrap() {
        return this == AES128_KW || this == AES256_KW;
    }

    /**
     * Check if this is a BLS signature algorithm.
     * @return true if this is a BLS algorithm
     */
    public boolean isBls() {
        return this == BLS12_381_G1 || this == BLS12_381_G2 || this == BLS12_381_G1G2;
    }

    @Override
    public String toString() {
        return algorithmName;
    }
}