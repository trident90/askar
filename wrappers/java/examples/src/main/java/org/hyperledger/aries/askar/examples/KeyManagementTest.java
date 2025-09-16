package org.hyperledger.aries.askar.examples;

import org.hyperledger.aries.askar.*;
import java.security.SecureRandom;

/**
 * Test key management functions (keyGenerate, keyFromSeed, keyFree).
 */
public class KeyManagementTest {

    public static void main(String[] args) {
        try {
            System.out.println("=== Key Management Test ===");
            
            // Test 1: Generate a key
            System.out.println("1. Testing key generation...");
            try {
                long keyHandle = AskarNative.keyGenerate("ed25519", null, false);
                if (keyHandle != 0) {
                    System.out.println("✅ Key generation succeeded, handle: " + keyHandle);
                    
                    // Test 3: Free the generated key
                    System.out.println("3. Testing key free...");
                    AskarNative.keyFree(keyHandle);
                    System.out.println("✅ Key freed successfully");
                } else {
                    System.out.println("❌ Key generation failed");
                }
            } catch (Exception e) {
                System.out.println("❌ Key generation failed: " + e.getMessage());
            }
            
            // Test 2: Generate a key from seed
            System.out.println("2. Testing key from seed...");
            try {
                // Create a 32-byte seed for Ed25519
                byte[] seed = new byte[32];
                new SecureRandom().nextBytes(seed);
                
                long keyHandle = AskarNative.keyFromSeed("ed25519", seed, null);
                if (keyHandle != 0) {
                    System.out.println("✅ Key from seed succeeded, handle: " + keyHandle);
                    
                    // Free this key too
                    AskarNative.keyFree(keyHandle);
                    System.out.println("✅ Seed-based key freed successfully");
                } else {
                    System.out.println("❌ Key from seed failed");
                }
            } catch (Exception e) {
                System.out.println("❌ Key from seed failed: " + e.getMessage());
            }
            
            System.out.println("\n🎉 Key Management Test completed!");
            
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}