package org.hyperledger.aries.askar.examples;

import org.hyperledger.aries.askar.*;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Comprehensive test for advanced key operations.
 */
public class AdvancedKeyOperationsTest {

    public static void main(String[] args) {
        try {
            System.out.println("=== Advanced Key Operations Test ===");
            
            // Test 1: Generate a key and inspect its properties
            System.out.println("\n1. Testing key generation and inspection...");
            long keyHandle = AskarNative.keyGenerate("ed25519", null, false);
            if (keyHandle == 0) {
                System.out.println("❌ Key generation failed");
                return;
            }
            System.out.println("✅ Key generated, handle: " + keyHandle);
            
            // Test 2: Get key algorithm
            System.out.println("\n2. Testing keyGetAlgorithm...");
            try {
                String algorithm = AskarNative.keyGetAlgorithm(keyHandle);
                if (algorithm != null) {
                    System.out.println("✅ Key algorithm: " + algorithm);
                } else {
                    System.out.println("❌ Algorithm is null");
                }
            } catch (Exception e) {
                System.out.println("❌ keyGetAlgorithm failed: " + e.getMessage());
            }
            
            // Test 3: Get public bytes
            System.out.println("\n3. Testing keyGetPublicBytes...");
            byte[] publicBytes = null;
            try {
                publicBytes = AskarNative.keyGetPublicBytes(keyHandle);
                if (publicBytes != null && publicBytes.length > 0) {
                    System.out.println("✅ Public bytes length: " + publicBytes.length);
                    System.out.println("   First few bytes: " + bytesToHex(Arrays.copyOf(publicBytes, Math.min(8, publicBytes.length))));
                } else {
                    System.out.println("❌ Public bytes is null or empty");
                }
            } catch (Exception e) {
                System.out.println("❌ keyGetPublicBytes failed: " + e.getMessage());
            }
            
            // Test 4: Get secret bytes
            System.out.println("\n4. Testing keyGetSecretBytes...");
            byte[] secretBytes = null;
            try {
                secretBytes = AskarNative.keyGetSecretBytes(keyHandle);
                if (secretBytes != null && secretBytes.length > 0) {
                    System.out.println("✅ Secret bytes length: " + secretBytes.length);
                    System.out.println("   First few bytes: " + bytesToHex(Arrays.copyOf(secretBytes, Math.min(8, secretBytes.length))));
                } else {
                    System.out.println("❌ Secret bytes is null or empty");
                }
            } catch (Exception e) {
                System.out.println("❌ keyGetSecretBytes failed: " + e.getMessage());
            }
            
            // Test 5: Get JWK public
            System.out.println("\n5. Testing keyGetJwkPublic...");
            String jwkPublic = null;
            try {
                jwkPublic = AskarNative.keyGetJwkPublic(keyHandle, null);
                if (jwkPublic != null && !jwkPublic.isEmpty()) {
                    System.out.println("✅ JWK Public: " + jwkPublic.substring(0, Math.min(100, jwkPublic.length())) + "...");
                } else {
                    System.out.println("❌ JWK Public is null or empty");
                }
            } catch (Exception e) {
                System.out.println("❌ keyGetJwkPublic failed: " + e.getMessage());
            }
            
            // Test 6: Get JWK secret
            System.out.println("\n6. Testing keyGetJwkSecret...");
            byte[] jwkSecret = null;
            try {
                jwkSecret = AskarNative.keyGetJwkSecret(keyHandle);
                if (jwkSecret != null && jwkSecret.length > 0) {
                    System.out.println("✅ JWK Secret length: " + jwkSecret.length);
                    String jwkSecretString = new String(jwkSecret);
                    System.out.println("   JWK Secret: " + jwkSecretString.substring(0, Math.min(100, jwkSecretString.length())) + "...");
                } else {
                    System.out.println("❌ JWK Secret is null or empty");
                }
            } catch (Exception e) {
                System.out.println("❌ keyGetJwkSecret failed: " + e.getMessage());
            }
            
            // Test 7: Create key from public bytes (if we got them)
            if (publicBytes != null) {
                System.out.println("\n7. Testing keyFromPublicBytes...");
                try {
                    long publicKeyHandle = AskarNative.keyFromPublicBytes("ed25519", publicBytes);
                    if (publicKeyHandle != 0) {
                        System.out.println("✅ Key from public bytes succeeded, handle: " + publicKeyHandle);
                        
                        // Verify it's the same algorithm
                        String pubAlgorithm = AskarNative.keyGetAlgorithm(publicKeyHandle);
                        System.out.println("   Public key algorithm: " + pubAlgorithm);
                        
                        AskarNative.keyFree(publicKeyHandle);
                        System.out.println("✅ Public key freed");
                    } else {
                        System.out.println("❌ Key from public bytes failed");
                    }
                } catch (Exception e) {
                    System.out.println("❌ keyFromPublicBytes failed: " + e.getMessage());
                }
            }
            
            // Test 8: Create key from secret bytes (if we got them)
            if (secretBytes != null) {
                System.out.println("\n8. Testing keyFromSecretBytes...");
                try {
                    long secretKeyHandle = AskarNative.keyFromSecretBytes("ed25519", secretBytes);
                    if (secretKeyHandle != 0) {
                        System.out.println("✅ Key from secret bytes succeeded, handle: " + secretKeyHandle);
                        
                        // Verify it's the same algorithm
                        String secAlgorithm = AskarNative.keyGetAlgorithm(secretKeyHandle);
                        System.out.println("   Secret key algorithm: " + secAlgorithm);
                        
                        AskarNative.keyFree(secretKeyHandle);
                        System.out.println("✅ Secret key freed");
                    } else {
                        System.out.println("❌ Key from secret bytes failed");
                    }
                } catch (Exception e) {
                    System.out.println("❌ keyFromSecretBytes failed: " + e.getMessage());
                }
            }
            
            // Test 9: Create key from JWK (if we got it)
            if (jwkSecret != null) {
                System.out.println("\n9. Testing keyFromJwk...");
                try {
                    long jwkKeyHandle = AskarNative.keyFromJwk(jwkSecret);
                    if (jwkKeyHandle != 0) {
                        System.out.println("✅ Key from JWK succeeded, handle: " + jwkKeyHandle);
                        
                        // Verify it's the same algorithm
                        String jwkAlgorithm = AskarNative.keyGetAlgorithm(jwkKeyHandle);
                        System.out.println("   JWK key algorithm: " + jwkAlgorithm);
                        
                        AskarNative.keyFree(jwkKeyHandle);
                        System.out.println("✅ JWK key freed");
                    } else {
                        System.out.println("❌ Key from JWK failed");
                    }
                } catch (Exception e) {
                    System.out.println("❌ keyFromJwk failed: " + e.getMessage());
                }
            }
            
            // Test 10: Test different key algorithms
            System.out.println("\n10. Testing different algorithms...");
            String[] algorithms = {"x25519", "secp256k1", "p256"};
            
            for (String alg : algorithms) {
                try {
                    System.out.println("   Testing algorithm: " + alg);
                    long algKeyHandle = AskarNative.keyGenerate(alg, null, false);
                    if (algKeyHandle != 0) {
                        String detectedAlg = AskarNative.keyGetAlgorithm(algKeyHandle);
                        System.out.println("   ✅ " + alg + " key generated, detected as: " + detectedAlg);
                        AskarNative.keyFree(algKeyHandle);
                    } else {
                        System.out.println("   ❌ " + alg + " key generation failed");
                    }
                } catch (Exception e) {
                    System.out.println("   ❌ " + alg + " failed: " + e.getMessage());
                }
            }
            
            // Free the original key
            AskarNative.keyFree(keyHandle);
            System.out.println("\n✅ Original key freed");
            
            System.out.println("\n🎉 Advanced Key Operations Test completed!");
            
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}