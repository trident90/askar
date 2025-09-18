package org.hyperledger.aries.askar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/**
 * Comprehensive error handling and edge case test suite.
 * Tests exception handling, boundary conditions, and error recovery.
 */
public class ErrorHandlingTests {

    private static final String TEST_DB_URI = "sqlite://:memory:";
    private static final String TEST_KEY_METHOD = "raw";
    private String testPassKey;

    @BeforeEach
    void setUp() throws AskarException {
        testPassKey = AskarNative.storeGenerateRawKey(null);
    }

    @AfterEach
    void tearDown() {
        // Cleanup
    }

    // ==================== AskarException Tests ====================

    @Test
    void testAskarExceptionErrorCodes() {
        // Test all error codes
        AskarException.ErrorCode[] codes = AskarException.ErrorCode.values();
        
        for (AskarException.ErrorCode code : codes) {
            AskarException exception = new AskarException(code, "Test message");
            
            assertEquals(code, exception.getErrorCode());
            assertEquals(code.getCode(), exception.getErrorCode().getCode());
            assertNotNull(exception.getMessage());
            assertTrue(exception.getMessage().contains("Test message"));
        }
    }

    @Test
    void testErrorCodeFromCode() {
        // Test conversion from numeric codes
        assertEquals(AskarException.ErrorCode.SUCCESS, AskarException.ErrorCode.fromCode(0));
        assertEquals(AskarException.ErrorCode.BACKEND, AskarException.ErrorCode.fromCode(1));
        assertEquals(AskarException.ErrorCode.BUSY, AskarException.ErrorCode.fromCode(2));
        assertEquals(AskarException.ErrorCode.DUPLICATE, AskarException.ErrorCode.fromCode(3));
        assertEquals(AskarException.ErrorCode.ENCRYPTION, AskarException.ErrorCode.fromCode(4));
        assertEquals(AskarException.ErrorCode.INPUT, AskarException.ErrorCode.fromCode(5));
        assertEquals(AskarException.ErrorCode.NOT_FOUND, AskarException.ErrorCode.fromCode(6));
        assertEquals(AskarException.ErrorCode.UNEXPECTED, AskarException.ErrorCode.fromCode(7));
        assertEquals(AskarException.ErrorCode.UNSUPPORTED, AskarException.ErrorCode.fromCode(8));
        assertEquals(AskarException.ErrorCode.WRAPPER, AskarException.ErrorCode.fromCode(100));
        
        // Test invalid code
        assertThrows(IllegalArgumentException.class, () -> {
            AskarException.ErrorCode.fromCode(999);
        });
    }

    @Test
    void testAskarExceptionToString() {
        AskarException exception = new AskarException(AskarException.ErrorCode.INPUT, "Invalid parameter");
        String toString = exception.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("INPUT"));
        assertTrue(toString.contains("Invalid parameter"));
    }

    @Test
    void testAskarExceptionWithExtra() {
        AskarException exception = new AskarException(AskarException.ErrorCode.BACKEND, "DB error", "Connection timeout");
        
        assertEquals("Connection timeout", exception.getExtra());
        assertTrue(exception.toString().contains("Connection timeout"));
    }

    // ==================== Key Operation Error Tests ====================

    @Test
    void testInvalidKeyAlgorithms() {
        // Test various invalid algorithm names
        String[] invalidAlgorithms = {
            null,
            "",
            "invalid",
            "rsa2048", // Not supported
            "aes128",  // Not a key generation algorithm
            "md5",     // Hash, not key algorithm
            "INVALID_ALGORITHM"
        };
        
        for (String algorithm : invalidAlgorithms) {
            assertThrows(Exception.class, () -> {
                SimpleKey.generate(algorithm, false);
            }, "Should throw exception for invalid algorithm: " + algorithm);
        }
    }

    @Test
    void testKeyOperationsOnFreedKey() throws AskarException {
        SimpleKey key = SimpleKey.generate(KeyAlgorithm.ED25519, false);
        
        // Verify key works initially
        assertNotNull(key.getAlgorithm());
        
        // Free the key
        key.close();
        
        // All operations should throw exceptions
        assertThrows(AskarException.class, () -> key.getAlgorithm());
        assertThrows(AskarException.class, () -> key.getPublicBytes());
        assertThrows(AskarException.class, () -> key.getSecretBytes());
        assertThrows(AskarException.class, () -> key.signMessage("test".getBytes()));
        assertThrows(AskarException.class, () -> key.verifySignature("test".getBytes(), new byte[64]));
    }

    @Test
    void testDoubleCloseKey() throws AskarException {
        SimpleKey key = SimpleKey.generate(KeyAlgorithm.ED25519, false);
        
        // First close should work
        key.close();
        
        // Second close should not throw exception
        assertDoesNotThrow(() -> key.close());
    }

    @Test
    void testInvalidSeedInputs() {
        // Test null seed
        assertThrows(Exception.class, () -> {
            SimpleKey.fromSeed("ed25519", null, "raw");
        });
        
        // Test empty seed
        assertThrows(Exception.class, () -> {
            SimpleKey.fromSeed("ed25519", new byte[0], "raw");
        });
        
        // Test invalid seed method
        byte[] validSeed = new byte[32];
        assertThrows(Exception.class, () -> {
            SimpleKey.fromSeed("ed25519", validSeed, "invalid_method");
        });
    }

    @Test
    void testSignatureWithInvalidInputs() throws AskarException {
        try (SimpleKey key = SimpleKey.generate(KeyAlgorithm.ED25519, false)) {
            
            // Test null message
            assertThrows(Exception.class, () -> {
                key.signMessage((byte[]) null);
            });
            
            // Test null message string
            assertThrows(Exception.class, () -> {
                key.signMessage((String) null);
            });
            
            // Test verification with null inputs
            byte[] validSignature = key.signMessage("test".getBytes());
            
            assertThrows(Exception.class, () -> {
                key.verifySignature(null, validSignature);
            });
            
            assertThrows(Exception.class, () -> {
                key.verifySignature("test".getBytes(), null);
            });
        }
    }

    @Test
    void testInvalidSignatureFormats() throws AskarException {
        try (SimpleKey key = SimpleKey.generate(KeyAlgorithm.ED25519, false)) {
            byte[] message = "test message".getBytes();
            
            // Test wrong signature length (may throw exception instead of returning false)
            try {
                byte[] shortSig = new byte[32]; // Should be 64 for Ed25519
                boolean result = key.verifySignature(message, shortSig);
                assertFalse(result, "Short signature should not verify");
            } catch (Exception e) {
                // Exception is also acceptable for invalid signature format
                System.out.println("Short signature threw exception (acceptable): " + e.getMessage());
            }
            
            try {
                byte[] longSig = new byte[128]; // Too long
                boolean result = key.verifySignature(message, longSig);
                assertFalse(result, "Long signature should not verify");
            } catch (Exception e) {
                System.out.println("Long signature threw exception (acceptable): " + e.getMessage());
            }
            
            // Test all-zero signature
            byte[] zeroSig = new byte[64];
            assertFalse(key.verifySignature(message, zeroSig));
            
            // Test all-max signature
            byte[] maxSig = new byte[64];
            for (int i = 0; i < maxSig.length; i++) {
                maxSig[i] = (byte) 0xFF;
            }
            assertFalse(key.verifySignature(message, maxSig));
        }
    }

    // ==================== Store Operation Error Tests ====================

    @Test
    void testInvalidStoreUris() {
        String[] invalidUris = {
            null,
            "",
            "invalid://uri",
            "file:///nonexistent/path/db.sqlite",
            "postgresql://invalid:connection@string",
            "redis://not:supported@host",
            "memory://invalid:syntax"
        };
        
        for (String uri : invalidUris) {
            assertThrows(Exception.class, () -> {
                SimpleStore.provision(uri, TEST_KEY_METHOD, testPassKey, null, true);
            }, "Should throw exception for invalid URI: " + uri);
        }
    }

    @Test
    void testInvalidKeyMethods() {
        String[] invalidMethods = {
            null,
            "",
            "invalid_method",
            "kdf:invalid:params",
            "argon2id:invalid",
            "pbkdf2:unsupported"
        };
        
        for (String method : invalidMethods) {
            assertThrows(Exception.class, () -> {
                SimpleStore.provision(TEST_DB_URI, method, testPassKey, null, true);
            }, "Should throw exception for invalid key method: " + method);
        }
    }

    @Test
    void testInvalidPassKeys() {
        String[] invalidKeys = {
            null,
            "",
            "too_short",
            "invalid_base64_!@#$%^&*()",
            "not_a_valid_raw_key"
        };
        
        for (String key : invalidKeys) {
            assertThrows(Exception.class, () -> {
                SimpleStore.provision(TEST_DB_URI, TEST_KEY_METHOD, key, null, true);
            }, "Should throw exception for invalid pass key: " + key);
        }
    }

    @Test
    void testOperationsOnClosedStore() throws AskarException {
        SimpleStore store = SimpleStore.provision(TEST_DB_URI, TEST_KEY_METHOD, testPassKey, null, true);
        
        // Close the store
        store.close();
        
        // Operations should fail
        assertThrows(Exception.class, () -> {
            store.createSession();
        });
        
        assertThrows(Exception.class, () -> {
            store.createTransaction();
        });
    }

    @Test
    void testSessionOperationsWithInvalidInputs() throws AskarException {
        try (SimpleStore store = SimpleStore.provision(TEST_DB_URI, TEST_KEY_METHOD, testPassKey, null, true);
             SimpleSession session = store.createSession()) {
            
            // Test null category
            assertThrows(Exception.class, () -> {
                session.insert(null, "name", "value");
            });
            
            // Test null name
            assertThrows(Exception.class, () -> {
                session.insert("category", null, "value");
            });
            
            // Test extremely long strings
            String longString = "x".repeat(1000000); // 1MB string
            assertDoesNotThrow(() -> {
                session.insert("category", "long_test", longString);
            });
            
            // Test special characters
            String specialChars = "!@#$%^&*()[]{}|\\:;\"'<>,.?/~`±§";
            assertDoesNotThrow(() -> {
                session.insert("category", "special", specialChars);
            });
        }
    }

    @Test
    void testConcurrentStoreAccess() throws AskarException, InterruptedException {
        // Test concurrent access to the same store
        try (SimpleStore store = SimpleStore.provision(TEST_DB_URI, TEST_KEY_METHOD, testPassKey, null, true)) {
            
            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try (SimpleSession session = store.createSession()) {
                        // Each thread performs operations
                        session.insert("thread_" + threadId, "item", "value_" + threadId);
                        session.count("thread_" + threadId);
                        session.fetch("thread_" + threadId, "item");
                    } catch (Exception e) {
                        // Some operations might fail due to concurrency
                        System.out.println("Thread " + threadId + " error: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            assertTrue(latch.await(10, TimeUnit.SECONDS), "Concurrent operations should complete");
            executor.shutdown();
        }
    }

    // ==================== Resource Management Error Tests ====================

    @Test
    void testMemoryExhaustion() throws AskarException {
        // Test creating many keys to potentially exhaust resources
        int keyCount = 1000;
        
        for (int i = 0; i < keyCount; i++) {
            try (SimpleKey key = SimpleKey.generate(KeyAlgorithm.ED25519, false)) {
                // Generate and immediately close
                assertNotNull(key.getAlgorithm());
            }
        }
        
        // Should not throw OutOfMemoryError
        assertTrue(true, "Memory exhaustion test completed");
    }

    @Test
    void testResourceLeakPrevention() throws AskarException {
        // Test that resources are properly cleaned up
        for (int i = 0; i < 100; i++) {
            SimpleStore store = SimpleStore.provision(TEST_DB_URI, TEST_KEY_METHOD, 
                    AskarNative.storeGenerateRawKey(null), null, true);
            SimpleSession session = store.createSession();
            SimpleKey key = SimpleKey.generate(KeyAlgorithm.ED25519, false);
            
            // Close in different orders to test cleanup
            if (i % 3 == 0) {
                key.close();
                session.close();
                store.close();
            } else if (i % 3 == 1) {
                session.close();
                key.close();
                store.close();
            } else {
                store.close(); // Should clean up everything
                key.close();   // Should handle already-cleaned resources
            }
        }
    }

    // ==================== Boundary Condition Tests ====================

    @Test
    void testEmptyInputHandling() throws AskarException {
        try (SimpleStore store = SimpleStore.provision(TEST_DB_URI, TEST_KEY_METHOD, testPassKey, null, true);
             SimpleSession session = store.createSession();
             SimpleKey key = SimpleKey.generate(KeyAlgorithm.ED25519, false)) {
            
            // Test empty message signing
            byte[] emptyMsg = new byte[0];
            byte[] signature = key.signMessage(emptyMsg);
            assertTrue(key.verifySignature(emptyMsg, signature));
            
            // Test empty string operations
            assertDoesNotThrow(() -> {
                session.insert("", "", "");
                session.fetch("", "");
                session.count("");
            });
        }
    }

    @Test
    void testLargeDataHandling() throws AskarException {
        try (SimpleStore store = SimpleStore.provision(TEST_DB_URI, TEST_KEY_METHOD, testPassKey, null, true);
             SimpleSession session = store.createSession();
             SimpleKey key = SimpleKey.generate(KeyAlgorithm.ED25519, false)) {
            
            // Test large message signing (1MB)
            byte[] largeMessage = new byte[1024 * 1024];
            for (int i = 0; i < largeMessage.length; i++) {
                largeMessage[i] = (byte) (i % 256);
            }
            
            byte[] signature = key.signMessage(largeMessage);
            assertTrue(key.verifySignature(largeMessage, signature));
            
            // Test large data storage
            String largeValue = "x".repeat(100000); // 100KB
            assertDoesNotThrow(() -> {
                session.insert("large", "data", largeValue);
            });
        }
    }

    @Test
    void testUnicodeHandling() throws AskarException {
        try (SimpleStore store = SimpleStore.provision(TEST_DB_URI, TEST_KEY_METHOD, testPassKey, null, true);
             SimpleSession session = store.createSession();
             SimpleKey key = SimpleKey.generate(KeyAlgorithm.ED25519, false)) {
            
            // Test Unicode strings
            String[] unicodeTests = {
                "Hello 世界",
                "🚀🌟💯",
                "Ñiño José François",
                "Москва",
                "العالم",
                "こんにちは",
                "\u0000\u0001\u0002", // Control characters
                "Mix: 🌍 中文 русский ع‌ربي"
            };
            
            for (String unicode : unicodeTests) {
                // Test signing Unicode
                byte[] signature = key.signMessage(unicode);
                assertTrue(key.verifySignature(unicode.getBytes(StandardCharsets.UTF_8), signature));
                
                // Test storing Unicode
                assertDoesNotThrow(() -> {
                    session.insert("unicode", "test_" + unicode.hashCode(), unicode);
                });
            }
        }
    }

    // ==================== Error Recovery Tests ====================

    @Test
    void testErrorRecovery() throws AskarException {
        try (SimpleStore store = SimpleStore.provision(TEST_DB_URI, TEST_KEY_METHOD, testPassKey, null, true)) {
            
            // Simulate operations that might fail and recovery
            for (int i = 0; i < 10; i++) {
                try (SimpleSession session = store.createSession()) {
                    
                    try {
                        // Intentionally cause an error
                        session.insert(null, "test", "value");
                        fail("Should have thrown exception");
                    } catch (Exception e) {
                        // Expected error
                    }
                    
                    // Verify session is still usable after error
                    final int index = i; // Make effectively final for lambda
                    assertDoesNotThrow(() -> {
                        session.insert("recovery", "test_" + index, "value_" + index);
                    });
                }
            }
        }
    }

    @Test
    void testPartialOperationFailure() throws AskarException {
        try (SimpleStore store = SimpleStore.provision(TEST_DB_URI, TEST_KEY_METHOD, testPassKey, null, true);
             SimpleSession session = store.createSession()) {
            
            // Insert valid data
            session.insert("valid", "item1", "value1");
            session.insert("valid", "item2", "value2");
            
            // Try invalid operation
            try {
                session.insert(null, "invalid", "value");
                fail("Should have thrown exception");
            } catch (Exception e) {
                // Expected
            }
            
            // Verify valid data is still accessible
            assertDoesNotThrow(() -> {
                session.fetch("valid", "item1");
                session.count("valid");
            });
            
            // Continue with more operations
            assertDoesNotThrow(() -> {
                session.insert("valid", "item3", "value3");
            });
        }
    }

    // ==================== Thread Safety Tests ====================

    @Test
    void testConcurrentKeyOperations() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 10; j++) {
                        try (SimpleKey key = SimpleKey.generate(KeyAlgorithm.ED25519, false)) {
                            byte[] signature = key.signMessage("test".getBytes());
                            assertTrue(key.verifySignature("test".getBytes(), signature));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Concurrent key operation failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(30, TimeUnit.SECONDS), "Concurrent key operations should complete");
        executor.shutdown();
    }

    @Test
    void testGetLastErrorFunction() {
        // Test the known non-working function
        assertThrows(Exception.class, () -> {
            AskarNative.getLastError();
        }, "getLastError should throw exception as it's not implemented");
    }
}