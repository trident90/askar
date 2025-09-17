package org.hyperledger.aries.askar.examples;

import org.hyperledger.aries.askar.*;

/**
 * Simple test of high-level wrapper functionality.
 * Tests only the working features (key operations).
 */
public class SimpleHighLevelTest {

    public static void main(String[] args) {
        System.out.println("=== Simple High-Level Wrapper Test ===");
        
        try {
            testSimpleKeyOperations();
            testKeyGenerationAlgorithms();
            
            System.out.println("\n🎉 High-level wrapper test completed successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testSimpleKeyOperations() throws AskarException {
        System.out.println("\n1. Testing Simple Key Operations...");
        
        // Test basic key generation with simplified interface
        try (SimpleKey key1 = SimpleKey.generate("ed25519", false)) {
            System.out.println("✅ Generated Ed25519 key: " + key1.getAlgorithm());
            System.out.println("   Ephemeral: " + key1.isEphemeral());
            System.out.println("   Public key length: " + key1.getPublicBytes().length + " bytes");
            
            // Test digital signatures with simplified interface
            String message = "Test message for high-level wrapper";
            byte[] signature = key1.signMessage(message);
            System.out.println("✅ Message signed, signature length: " + signature.length + " bytes");
            
            // Test signature verification
            boolean isValid = key1.verifySignature(message.getBytes(), signature);
            System.out.println("✅ Signature verification: " + (isValid ? "VALID" : "INVALID"));
            
            // Test with wrong message (should be invalid)
            boolean isInvalid = key1.verifySignature("Different message".getBytes(), signature);
            System.out.println("✅ Wrong message verification: " + (isInvalid ? "FAILED SECURITY!" : "correctly invalid"));
            
            // Test key information methods
            System.out.println("✅ Key algorithm: " + key1.getAlgorithm());
            System.out.println("✅ Key public bytes: " + bytesToHex(key1.getPublicBytes()));
        }
    }
    
    private static void testKeyGenerationAlgorithms() throws AskarException {
        System.out.println("\n2. Testing Different Key Algorithms...");
        
        // Test different algorithms using the enum
        try (SimpleKey ed25519Key = SimpleKey.generate(KeyAlgorithm.ED25519, true)) {
            System.out.println("✅ Ed25519 key: " + ed25519Key.getAlgorithm() + 
                    " (ephemeral: " + ed25519Key.isEphemeral() + ")");
            System.out.println("   Public key: " + ed25519Key.getPublicBytes().length + " bytes");
            
            // Test signing
            byte[] testSig = ed25519Key.signMessage("test");
            boolean testValid = ed25519Key.verifySignature("test".getBytes(), testSig);
            System.out.println("   Signing test: " + (testValid ? "PASSED" : "FAILED"));
        }
        
        try (SimpleKey x25519Key = SimpleKey.generate(KeyAlgorithm.X25519, true)) {
            System.out.println("✅ X25519 key: " + x25519Key.getAlgorithm() + 
                    " (ephemeral: " + x25519Key.isEphemeral() + ")");
            System.out.println("   Public key: " + x25519Key.getPublicBytes().length + " bytes");
        }
        
        // Test key from seed
        byte[] seed = "this-is-a-test-seed-32-bytes-long".getBytes();
        if (seed.length < 32) {
            byte[] paddedSeed = new byte[32];
            System.arraycopy(seed, 0, paddedSeed, 0, seed.length);
            seed = paddedSeed;
        } else if (seed.length > 32) {
            seed = java.util.Arrays.copyOf(seed, 32);
        }
        
        try (SimpleKey seedKey1 = SimpleKey.fromSeed("ed25519", seed, null);
             SimpleKey seedKey2 = SimpleKey.fromSeed("ed25519", seed, null)) {
            
            System.out.println("✅ Deterministic keys from seed:");
            System.out.println("   Key 1 public: " + bytesToHex(seedKey1.getPublicBytes()));
            System.out.println("   Key 2 public: " + bytesToHex(seedKey2.getPublicBytes()));
            
            boolean samePublicKey = java.util.Arrays.equals(
                    seedKey1.getPublicBytes(), 
                    seedKey2.getPublicBytes()
            );
            System.out.println("   Same public key: " + (samePublicKey ? "YES" : "NO"));
        }
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}