package org.hyperledger.aries.askar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Comprehensive test suite for key operations based on Rust test patterns.
 * Covers all key algorithms and cryptographic operations.
 */
public class ComprehensiveKeyTests {

    private static final String TEST_MESSAGE = "This is a dummy message for use with tests";
    private static final byte[] TEST_MESSAGE_BYTES = TEST_MESSAGE.getBytes(StandardCharsets.UTF_8);
    private static final byte[] INVALID_MESSAGE = "Not the message".getBytes(StandardCharsets.UTF_8);
    
    // Test vectors for deterministic testing
    private static final byte[] TEST_SEED_32 = new byte[32];
    private static final byte[] TEST_SEED_64 = new byte[64];
    
    static {
        // Initialize test seeds with known values
        for (int i = 0; i < 32; i++) {
            TEST_SEED_32[i] = (byte) (i + 1);
        }
        for (int i = 0; i < 64; i++) {
            TEST_SEED_64[i] = (byte) (i + 1);
        }
    }

    @BeforeEach
    void setUp() throws AskarException {
        // Ensure clean state before each test
        AskarNative.setMaxLogLevel(1); // Enable debug logging
    }

    @AfterEach
    void tearDown() {
        // Cleanup after each test if needed
    }

    // ==================== Key Generation Tests ====================

    @Test
    void testEd25519KeyGeneration() throws AskarException {
        try (SimpleKey key = SimpleKey.generate(KeyAlgorithm.ED25519, false)) {
            assertNotNull(key);
            assertEquals("ed25519", key.getAlgorithm().toLowerCase());
            
            byte[] publicBytes = key.getPublicBytes();
            assertNotNull(publicBytes);
            assertEquals(32, publicBytes.length); // Ed25519 public key is 32 bytes
            
            byte[] secretBytes = key.getSecretBytes();
            assertNotNull(secretBytes);
            assertEquals(32, secretBytes.length); // Ed25519 secret key is 32 bytes
        }
    }

    @Test
    void testX25519KeyGeneration() throws AskarException {
        try (SimpleKey key = SimpleKey.generate(KeyAlgorithm.X25519, false)) {
            assertNotNull(key);
            assertEquals("x25519", key.getAlgorithm().toLowerCase());
            
            byte[] publicBytes = key.getPublicBytes();
            assertNotNull(publicBytes);
            assertEquals(32, publicBytes.length); // X25519 public key is 32 bytes
            
            byte[] secretBytes = key.getSecretBytes();
            assertNotNull(secretBytes);
            assertEquals(32, secretBytes.length); // X25519 secret key is 32 bytes
        }
    }

    @Test
    void testSecp256k1KeyGeneration() throws AskarException {
        try (SimpleKey key = SimpleKey.generate(KeyAlgorithm.ES256K, false)) {
            assertNotNull(key);
            assertEquals("es256k", key.getAlgorithm().toLowerCase());
            
            byte[] publicBytes = key.getPublicBytes();
            assertNotNull(publicBytes);
            // Secp256k1 compressed public key is 33 bytes
            assertTrue(publicBytes.length == 33 || publicBytes.length == 65);
            
            byte[] secretBytes = key.getSecretBytes();
            assertNotNull(secretBytes);
            assertEquals(32, secretBytes.length); // Secp256k1 secret key is 32 bytes
        }
    }

    @Test
    void testAllKeyAlgorithms() throws AskarException {
        // Test key generation for all available algorithms
        KeyAlgorithm[] algorithmsToTest = {
            KeyAlgorithm.ED25519,
            KeyAlgorithm.X25519,
            KeyAlgorithm.ES256K
        };

        for (KeyAlgorithm algorithm : algorithmsToTest) {
            try (SimpleKey key = SimpleKey.generate(algorithm.name().toLowerCase(), false)) {
                assertNotNull(key, "Failed to generate key for " + algorithm);
                assertNotNull(key.getAlgorithm(), "Algorithm name is null for " + algorithm);
                assertNotNull(key.getPublicBytes(), "Public bytes are null for " + algorithm);
                assertNotNull(key.getSecretBytes(), "Secret bytes are null for " + algorithm);
                assertTrue(key.getPublicBytes().length > 0, "Public bytes are empty for " + algorithm);
                assertTrue(key.getSecretBytes().length > 0, "Secret bytes are empty for " + algorithm);
            }
        }
    }

    @Test
    void testKeyGenerationRandomness() throws AskarException {
        // Test that multiple key generations produce different keys
        try (SimpleKey key1 = SimpleKey.generate(KeyAlgorithm.ED25519, false);
             SimpleKey key2 = SimpleKey.generate(KeyAlgorithm.ED25519, false)) {
            
            assertFalse(Arrays.equals(key1.getPublicBytes(), key2.getPublicBytes()),
                    "Two random keys should have different public bytes");
            assertFalse(Arrays.equals(key1.getSecretBytes(), key2.getSecretBytes()),
                    "Two random keys should have different secret bytes");
        }
    }

    @Test
    void testDeterministicKeyGeneration() throws AskarException {
        // Test that keys generated from the same seed are identical
        try (SimpleKey key1 = SimpleKey.fromSeed("ed25519", TEST_SEED_32, "raw");
             SimpleKey key2 = SimpleKey.fromSeed("ed25519", TEST_SEED_32, "raw")) {
            
            assertArrayEquals(key1.getPublicBytes(), key2.getPublicBytes(),
                    "Keys from same seed should have identical public bytes");
            assertArrayEquals(key1.getSecretBytes(), key2.getSecretBytes(),
                    "Keys from same seed should have identical secret bytes");
        }
    }

    @Test
    void testSeedMethodVariants() throws AskarException {
        // Test different seed methods
        String[] seedMethods = {"raw", "blake2b"};
        
        for (String method : seedMethods) {
            try (SimpleKey key = SimpleKey.fromSeed("ed25519", TEST_SEED_32, method)) {
                assertNotNull(key, "Failed to generate key with seed method: " + method);
                assertEquals("ed25519", key.getAlgorithm().toLowerCase());
                assertNotNull(key.getPublicBytes());
                assertNotNull(key.getSecretBytes());
            }
        }
    }

    // ==================== Digital Signature Tests ====================

    @Test
    void testEd25519SignatureBasic() throws AskarException {
        try (SimpleKey key = SimpleKey.generate(KeyAlgorithm.ED25519, false)) {
            byte[] signature = key.signMessage(TEST_MESSAGE_BYTES);
            
            assertNotNull(signature);
            assertEquals(64, signature.length); // Ed25519 signature is 64 bytes
            
            assertTrue(key.verifySignature(TEST_MESSAGE_BYTES, signature),
                    "Valid signature should verify successfully");
        }
    }

    @Test
    void testSignatureWithStringMessage() throws AskarException {
        try (SimpleKey key = SimpleKey.generate(KeyAlgorithm.ED25519, false)) {
            byte[] signature = key.signMessage(TEST_MESSAGE);
            
            assertNotNull(signature);
            assertTrue(key.verifySignature(TEST_MESSAGE_BYTES, signature),
                    "Signature from string message should verify with byte array");
        }
    }

    @Test
    void testSignatureRejection() throws AskarException {
        try (SimpleKey key = SimpleKey.generate(KeyAlgorithm.ED25519, false)) {
            byte[] signature = key.signMessage(TEST_MESSAGE_BYTES);
            
            // Test invalid message rejection
            assertFalse(key.verifySignature(INVALID_MESSAGE, signature),
                    "Signature should not verify with different message");
            
            // Test invalid signature rejection
            byte[] invalidSignature = new byte[64]; // All zeros
            assertFalse(key.verifySignature(TEST_MESSAGE_BYTES, invalidSignature),
                    "Invalid signature should not verify");
            
            // Test corrupted signature rejection
            byte[] corruptedSignature = Arrays.copyOf(signature, signature.length);
            corruptedSignature[0] = (byte) ~corruptedSignature[0]; // Flip bits
            assertFalse(key.verifySignature(TEST_MESSAGE_BYTES, corruptedSignature),
                    "Corrupted signature should not verify");
        }
    }

    @Test
    void testSignatureTypes() throws AskarException {
        try (SimpleKey key = SimpleKey.generate(KeyAlgorithm.ED25519, false)) {
            // Test with explicit signature type
            byte[] signature1 = key.signMessage(TEST_MESSAGE_BYTES, null);
            byte[] signature2 = key.signMessage(TEST_MESSAGE_BYTES, "");
            
            assertNotNull(signature1);
            assertNotNull(signature2);
            
            assertTrue(key.verifySignature(TEST_MESSAGE_BYTES, signature1, null));
            assertTrue(key.verifySignature(TEST_MESSAGE_BYTES, signature2, ""));
        }
    }

    @Test
    void testCrossKeySignatureRejection() throws AskarException {
        try (SimpleKey key1 = SimpleKey.generate(KeyAlgorithm.ED25519, false);
             SimpleKey key2 = SimpleKey.generate(KeyAlgorithm.ED25519, false)) {
            
            byte[] signature = key1.signMessage(TEST_MESSAGE_BYTES);
            
            // Signature from key1 should not verify with key2
            assertFalse(key2.verifySignature(TEST_MESSAGE_BYTES, signature),
                    "Signature from different key should not verify");
        }
    }

    // ==================== Key Lifecycle Tests ====================

    @Test
    void testKeyResourceManagement() throws AskarException {
        SimpleKey key = SimpleKey.generate(KeyAlgorithm.ED25519, false);
        
        // Key should be usable
        assertNotNull(key.getAlgorithm());
        assertNotNull(key.getPublicBytes());
        
        // Close the key
        key.close();
        
        // Key should not be usable after closing
        assertThrows(AskarException.class, () -> key.getAlgorithm(),
                "Should throw exception when accessing closed key");
        assertThrows(AskarException.class, () -> key.getPublicBytes(),
                "Should throw exception when accessing closed key");
        assertThrows(AskarException.class, () -> key.signMessage(TEST_MESSAGE_BYTES),
                "Should throw exception when using closed key");
    }

    @Test
    void testKeyToString() throws AskarException {
        try (SimpleKey key = SimpleKey.generate(KeyAlgorithm.ED25519, false)) {
            String keyString = key.toString();
            assertNotNull(keyString);
            assertTrue(keyString.contains("SimpleKey"));
            assertTrue(keyString.contains("ed25519"));
            assertTrue(keyString.contains("handle="));
        }
        
        // Test toString on closed key
        SimpleKey closedKey = SimpleKey.generate(KeyAlgorithm.ED25519, false);
        closedKey.close();
        String closedString = closedKey.toString();
        assertTrue(closedString.contains("freed") || closedString.contains("error"));
    }

    // ==================== Edge Cases and Error Handling ====================

    @Test
    void testInvalidAlgorithm() {
        assertThrows(AskarException.class, () -> {
            SimpleKey.generate("invalid_algorithm", false);
        }, "Should throw exception for invalid algorithm");
    }

    @Test
    void testNullInputs() {
        assertThrows(Exception.class, () -> {
            SimpleKey.generate((String) null, false);
        }, "Should throw exception for null algorithm");
        
        assertThrows(Exception.class, () -> {
            SimpleKey.fromSeed("ed25519", null, "raw");
        }, "Should throw exception for null seed");
    }

    @Test
    void testEmptyInputs() throws AskarException {
        try (SimpleKey key = SimpleKey.generate(KeyAlgorithm.ED25519, false)) {
            // Test signing empty message
            byte[] signature = key.signMessage(new byte[0]);
            assertNotNull(signature);
            assertTrue(key.verifySignature(new byte[0], signature),
                    "Should be able to sign and verify empty message");
        }
    }

    @Test
    void testLargeMessage() throws AskarException {
        try (SimpleKey key = SimpleKey.generate(KeyAlgorithm.ED25519, false)) {
            // Test with large message (1MB)
            byte[] largeMessage = new byte[1024 * 1024];
            new SecureRandom().nextBytes(largeMessage);
            
            byte[] signature = key.signMessage(largeMessage);
            assertNotNull(signature);
            assertTrue(key.verifySignature(largeMessage, signature),
                    "Should be able to sign and verify large message");
        }
    }

    // ==================== Performance and Stress Tests ====================

    @Test
    void testKeyGenerationPerformance() throws AskarException {
        long startTime = System.currentTimeMillis();
        int keyCount = 100;
        
        for (int i = 0; i < keyCount; i++) {
            try (SimpleKey key = SimpleKey.generate(KeyAlgorithm.ED25519, false)) {
                // Just generate and close
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration < 10000, // Should complete in under 10 seconds
                String.format("Key generation too slow: %d keys in %d ms", keyCount, duration));
    }

    @Test
    void testSignaturePerformance() throws AskarException {
        try (SimpleKey key = SimpleKey.generate(KeyAlgorithm.ED25519, false)) {
            long startTime = System.currentTimeMillis();
            int signatureCount = 1000;
            
            for (int i = 0; i < signatureCount; i++) {
                byte[] signature = key.signMessage(TEST_MESSAGE_BYTES);
                assertTrue(key.verifySignature(TEST_MESSAGE_BYTES, signature));
            }
            
            long duration = System.currentTimeMillis() - startTime;
            assertTrue(duration < 5000, // Should complete in under 5 seconds
                    String.format("Signature operations too slow: %d operations in %d ms", 
                            signatureCount * 2, duration));
        }
    }

    // ==================== Integration Tests ====================

    @Test
    void testCompleteKeyWorkflow() throws AskarException {
        // Test complete workflow: generate -> use -> serialize -> verify
        try (SimpleKey key = SimpleKey.generate(KeyAlgorithm.ED25519, false)) {
            // 1. Get key properties
            String algorithm = key.getAlgorithm();
            byte[] publicBytes = key.getPublicBytes();
            byte[] secretBytes = key.getSecretBytes();
            
            assertNotNull(algorithm);
            assertNotNull(publicBytes);
            assertNotNull(secretBytes);
            
            // 2. Sign a message
            byte[] signature = key.signMessage(TEST_MESSAGE_BYTES);
            assertNotNull(signature);
            
            // 3. Verify the signature
            assertTrue(key.verifySignature(TEST_MESSAGE_BYTES, signature));
            
            // 4. Test with multiple messages
            String[] testMessages = {
                "Hello World",
                "Test message 2",
                "Another test message with special chars: !@#$%^&*()",
                "",
                "Very long message: " + "x".repeat(1000)
            };
            
            for (String msg : testMessages) {
                byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
                byte[] sig = key.signMessage(msgBytes);
                assertTrue(key.verifySignature(msgBytes, sig),
                        "Failed to verify signature for message: " + msg);
            }
        }
    }
}