package org.hyperledger.aries.askar;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Key functionality.
 */
public class KeyTest {

    @Test
    void testKeyGeneration() throws AskarException {
        try (Key key = Key.generate(KeyAlgorithm.ED25519, false)) {
            assertNotNull(key);
            assertEquals(KeyAlgorithm.ED25519, key.getAlgorithm());
            assertFalse(key.isEphemeral());

            // Test public key bytes
            byte[] publicBytes = key.getPublicBytes();
            assertNotNull(publicBytes);
            assertEquals(32, publicBytes.length); // Ed25519 public key size

            // Test secret key bytes
            byte[] secretBytes = key.getSecretBytes();
            assertNotNull(secretBytes);
            assertEquals(32, secretBytes.length); // Ed25519 secret key size
        }
    }

    @Test
    void testEphemeralKey() throws AskarException {
        try (Key key = Key.generate(KeyAlgorithm.ED25519, true)) {
            assertTrue(key.isEphemeral());
        }
    }

    @Test
    void testKeyFromSeed() throws AskarException {
        byte[] seed = new byte[32];
        for (int i = 0; i < seed.length; i++) {
            seed[i] = (byte) i;
        }

        try (Key key1 = Key.fromSeed(KeyAlgorithm.ED25519, seed, SeedMethod.BLAKE2B);
             Key key2 = Key.fromSeed(KeyAlgorithm.ED25519, seed, SeedMethod.BLAKE2B)) {

            assertNotNull(key1);
            assertNotNull(key2);

            // Keys generated from same seed should have same public bytes
            byte[] pubBytes1 = key1.getPublicBytes();
            byte[] pubBytes2 = key2.getPublicBytes();
            assertArrayEquals(pubBytes1, pubBytes2);
        }
    }

    @Test
    void testKeyFromSecretBytes() throws AskarException {
        // Generate a key first to get valid secret bytes
        try (Key originalKey = Key.generate(KeyAlgorithm.ED25519, false)) {
            byte[] secretBytes = originalKey.getSecretBytes();

            try (Key reconstructedKey = Key.fromSecretBytes(KeyAlgorithm.ED25519, secretBytes)) {
                assertNotNull(reconstructedKey);
                assertEquals(KeyAlgorithm.ED25519, reconstructedKey.getAlgorithm());

                // Public bytes should match
                byte[] originalPubBytes = originalKey.getPublicBytes();
                byte[] reconstructedPubBytes = reconstructedKey.getPublicBytes();
                assertArrayEquals(originalPubBytes, reconstructedPubBytes);
            }
        }
    }

    @Test
    void testKeyFromPublicBytes() throws AskarException {
        // Generate a key first to get valid public bytes
        try (Key originalKey = Key.generate(KeyAlgorithm.ED25519, false)) {
            byte[] publicBytes = originalKey.getPublicBytes();

            try (Key publicOnlyKey = Key.fromPublicBytes(KeyAlgorithm.ED25519, publicBytes)) {
                assertNotNull(publicOnlyKey);
                assertEquals(KeyAlgorithm.ED25519, publicOnlyKey.getAlgorithm());

                // Public bytes should match
                byte[] retrievedPubBytes = publicOnlyKey.getPublicBytes();
                assertArrayEquals(publicBytes, retrievedPubBytes);

                // Should not be able to get secret bytes from public-only key
                assertThrows(AskarException.class, () -> publicOnlyKey.getSecretBytes());
            }
        }
    }

    @Test
    void testJwkOperations() throws AskarException {
        try (Key key = Key.generate(KeyAlgorithm.ED25519, false)) {
            // Get JWK public
            String jwkPublic = key.getJwkPublic(null);
            assertNotNull(jwkPublic);
            assertFalse(jwkPublic.isEmpty());
            assertTrue(jwkPublic.contains("\"kty\""));
            assertTrue(jwkPublic.contains("\"crv\""));

            // Get JWK secret
            byte[] jwkSecret = key.getJwkSecret();
            assertNotNull(jwkSecret);
            assertTrue(jwkSecret.length > 0);

            // Create key from JWK
            try (Key keyFromJwk = Key.fromJwk(new String(jwkSecret))) {
                assertNotNull(keyFromJwk);
                assertEquals(KeyAlgorithm.ED25519, keyFromJwk.getAlgorithm());

                // Public bytes should match
                byte[] originalPubBytes = key.getPublicBytes();
                byte[] jwkPubBytes = keyFromJwk.getPublicBytes();
                assertArrayEquals(originalPubBytes, jwkPubBytes);
            }
        }
    }

    @Test
    void testSignAndVerify() throws AskarException {
        byte[] message = "Hello, Askar!".getBytes();

        try (Key key = Key.generate(KeyAlgorithm.ED25519, false)) {
            // Sign message
            byte[] signature = key.signMessage(message, null);
            assertNotNull(signature);
            assertTrue(signature.length > 0);

            // Verify signature
            boolean isValid = key.verifySignature(message, signature, null);
            assertTrue(isValid);

            // Verify with wrong message should fail
            byte[] wrongMessage = "Wrong message".getBytes();
            boolean isInvalid = key.verifySignature(wrongMessage, signature, null);
            assertFalse(isInvalid);

            // Test with public-only key
            byte[] publicBytes = key.getPublicBytes();
            try (Key publicKey = Key.fromPublicBytes(KeyAlgorithm.ED25519, publicBytes)) {
                // Should be able to verify
                boolean verifyWithPublic = publicKey.verifySignature(message, signature, null);
                assertTrue(verifyWithPublic);

                // Should not be able to sign
                assertThrows(AskarException.class, () -> publicKey.signMessage(message, null));
            }
        }
    }

    @Test
    void testKeyConversion() throws AskarException {
        try (Key ed25519Key = Key.generate(KeyAlgorithm.ED25519, false)) {
            // Convert to X25519 for key exchange
            try (Key x25519Key = ed25519Key.convert(KeyAlgorithm.X25519)) {
                assertNotNull(x25519Key);
                assertEquals(KeyAlgorithm.X25519, x25519Key.getAlgorithm());

                // Public bytes should be different but related
                byte[] ed25519PubBytes = ed25519Key.getPublicBytes();
                byte[] x25519PubBytes = x25519Key.getPublicBytes();
                assertEquals(ed25519PubBytes.length, x25519PubBytes.length);
                assertFalse(java.util.Arrays.equals(ed25519PubBytes, x25519PubBytes));
            }
        }
    }

    @Test
    void testAeadOperations() throws AskarException {
        try (Key key = Key.generate(KeyAlgorithm.CHACHA20_POLY1305, false)) {
            byte[] message = "Secret message".getBytes();
            byte[] aad = "Additional data".getBytes();

            // Generate random nonce
            // Note: AEAD operations not fully implemented in this version
            // byte[] nonce = key.aeadRandomNonce();
            // assertNotNull(nonce);
            // assertTrue(nonce.length > 0);

            // Encrypt
            // Note: This would require proper AskarLibrary.EncryptedData handling
            // which is simplified here for the test structure
        }
    }

    @Test
    void testKeyAlgorithmMethods() {
        assertTrue(KeyAlgorithm.ED25519.isEcdsa());
        assertFalse(KeyAlgorithm.ED25519.isEcdh());
        assertFalse(KeyAlgorithm.ED25519.isAead());

        assertTrue(KeyAlgorithm.X25519.isEcdh());
        assertFalse(KeyAlgorithm.X25519.isEcdsa());

        assertTrue(KeyAlgorithm.CHACHA20_POLY1305.isAead());
        assertFalse(KeyAlgorithm.CHACHA20_POLY1305.isEcdsa());

        assertTrue(KeyAlgorithm.AES128_KW.isKeyWrap());
        assertTrue(KeyAlgorithm.BLS12_381_G1.isBls());
    }

    @Test
    void testSeedMethods() {
        assertEquals("blake2b", SeedMethod.BLAKE2B.getMethodName());
        assertEquals("raw", SeedMethod.RAW.getMethodName());

        assertEquals(SeedMethod.BLAKE2B, SeedMethod.fromString("blake2b"));
        assertEquals(SeedMethod.RAW, SeedMethod.fromString("raw"));
        assertEquals(SeedMethod.BLAKE2B, SeedMethod.fromString(null)); // Default

        assertThrows(IllegalArgumentException.class, () -> SeedMethod.fromString("invalid"));
    }
}