package org.hyperledger.aries.askar.examples;

import org.hyperledger.aries.askar.*;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Comprehensive test for key derivation operations.
 */
public class KeyDerivationTest {

    public static void main(String[] args) {
        try {
            System.out.println("=== Key Derivation Test ===");
            
            // Test 1: Basic key exchange (simple case)
            System.out.println("\n1. Testing basic key exchange...");
            long secretKey1 = AskarNative.keyGenerate("x25519", null, false);
            long publicKey1 = AskarNative.keyGenerate("x25519", null, false);
            
            if (secretKey1 != 0 && publicKey1 != 0) {
                System.out.println("✅ Generated X25519 keys - Secret: " + secretKey1 + ", Public: " + publicKey1);
                
                try {
                    long derivedKey = AskarNative.keyFromKeyExchange("x25519", secretKey1, publicKey1);
                    if (derivedKey != 0) {
                        System.out.println("✅ Key exchange succeeded, derived key: " + derivedKey);
                        
                        // Check the derived key properties
                        String algorithm = AskarNative.keyGetAlgorithm(derivedKey);
                        System.out.println("   Derived key algorithm: " + algorithm);
                        
                        AskarNative.keyFree(derivedKey);
                        System.out.println("✅ Derived key freed");
                    } else {
                        System.out.println("❌ Key exchange failed");
                    }
                } catch (Exception e) {
                    System.out.println("❌ Key exchange failed: " + e.getMessage());
                }
                
                AskarNative.keyFree(secretKey1);
                AskarNative.keyFree(publicKey1);
            } else {
                System.out.println("❌ Failed to generate X25519 keys");
            }
            
            // Test 2: Key conversion
            System.out.println("\n2. Testing key conversion...");
            long ed25519Key = AskarNative.keyGenerate("ed25519", null, false);
            if (ed25519Key != 0) {
                System.out.println("✅ Generated Ed25519 key: " + ed25519Key);
                
                try {
                    // Try to convert Ed25519 to X25519 (common conversion)
                    long convertedKey = AskarNative.keyConvert(ed25519Key, "x25519");
                    if (convertedKey != 0) {
                        System.out.println("✅ Key conversion succeeded: Ed25519 → X25519, new key: " + convertedKey);
                        
                        String originalAlg = AskarNative.keyGetAlgorithm(ed25519Key);
                        String convertedAlg = AskarNative.keyGetAlgorithm(convertedKey);
                        System.out.println("   Original: " + originalAlg + " → Converted: " + convertedAlg);
                        
                        AskarNative.keyFree(convertedKey);
                        System.out.println("✅ Converted key freed");
                    } else {
                        System.out.println("❌ Key conversion failed");
                    }
                } catch (Exception e) {
                    System.out.println("❌ Key conversion failed: " + e.getMessage());
                }
                
                AskarNative.keyFree(ed25519Key);
            } else {
                System.out.println("❌ Failed to generate Ed25519 key");
            }
            
            // Test 3: ECDH-ES key derivation (simplified)
            System.out.println("\n3. Testing ECDH-ES key derivation...");
            try {
                long ephemeralKey = AskarNative.keyGenerate("x25519", null, true); // ephemeral
                long recipientKey = AskarNative.keyGenerate("x25519", null, false);
                
                if (ephemeralKey != 0 && recipientKey != 0) {
                    System.out.println("✅ Generated keys for ECDH-ES - Ephemeral: " + ephemeralKey + ", Recipient: " + recipientKey);
                    
                    // Test with minimal parameters (algorithm ID, apu, apv can be null for basic test)
                    byte[] algorithmId = "A256GCM".getBytes(); // Common algorithm identifier
                    
                    try {
                        long derivedKey = AskarNative.keyDeriveEcdhEs("aes256-gcm", ephemeralKey, recipientKey, 
                                                                     algorithmId, null, null, false);
                        if (derivedKey != 0) {
                            System.out.println("✅ ECDH-ES key derivation succeeded, derived key: " + derivedKey);
                            
                            String derivedAlg = AskarNative.keyGetAlgorithm(derivedKey);
                            System.out.println("   Derived key algorithm: " + derivedAlg);
                            
                            AskarNative.keyFree(derivedKey);
                            System.out.println("✅ ECDH-ES derived key freed");
                        } else {
                            System.out.println("❌ ECDH-ES key derivation failed");
                        }
                    } catch (Exception e) {
                        System.out.println("⚠️  ECDH-ES not supported or failed: " + e.getMessage());
                    }
                    
                    AskarNative.keyFree(ephemeralKey);
                    AskarNative.keyFree(recipientKey);
                } else {
                    System.out.println("❌ Failed to generate keys for ECDH-ES");
                }
            } catch (Exception e) {
                System.out.println("⚠️  ECDH-ES test failed: " + e.getMessage());
            }
            
            // Test 4: ECDH-1PU key derivation (simplified)
            System.out.println("\n4. Testing ECDH-1PU key derivation...");
            try {
                long ephemeralKey = AskarNative.keyGenerate("x25519", null, true);
                long senderKey = AskarNative.keyGenerate("x25519", null, false);
                long recipientKey = AskarNative.keyGenerate("x25519", null, false);
                
                if (ephemeralKey != 0 && senderKey != 0 && recipientKey != 0) {
                    System.out.println("✅ Generated keys for ECDH-1PU - Ephemeral: " + ephemeralKey + 
                                     ", Sender: " + senderKey + ", Recipient: " + recipientKey);
                    
                    byte[] algorithmId = "A256GCM".getBytes();
                    
                    try {
                        long derivedKey = AskarNative.keyDeriveEcdh1Pu("aes256-gcm", ephemeralKey, senderKey, recipientKey,
                                                                      algorithmId, null, null, null, false);
                        if (derivedKey != 0) {
                            System.out.println("✅ ECDH-1PU key derivation succeeded, derived key: " + derivedKey);
                            
                            String derivedAlg = AskarNative.keyGetAlgorithm(derivedKey);
                            System.out.println("   Derived key algorithm: " + derivedAlg);
                            
                            AskarNative.keyFree(derivedKey);
                            System.out.println("✅ ECDH-1PU derived key freed");
                        } else {
                            System.out.println("❌ ECDH-1PU key derivation failed");
                        }
                    } catch (Exception e) {
                        System.out.println("⚠️  ECDH-1PU not supported or failed: " + e.getMessage());
                    }
                    
                    AskarNative.keyFree(ephemeralKey);
                    AskarNative.keyFree(senderKey);
                    AskarNative.keyFree(recipientKey);
                } else {
                    System.out.println("❌ Failed to generate keys for ECDH-1PU");
                }
            } catch (Exception e) {
                System.out.println("⚠️  ECDH-1PU test failed: " + e.getMessage());
            }
            
            // Test 5: Random nonce generation
            System.out.println("\n5. Testing random nonce generation...");
            
            // Test crypto box random nonce (static function)
            try {
                byte[] cryptoBoxNonce = AskarNative.keyCryptoBoxRandomNonce();
                if (cryptoBoxNonce != null && cryptoBoxNonce.length > 0) {
                    System.out.println("✅ Crypto box random nonce: length=" + cryptoBoxNonce.length + 
                                     ", nonce=" + bytesToHex(Arrays.copyOf(cryptoBoxNonce, Math.min(8, cryptoBoxNonce.length))) + "...");
                } else {
                    System.out.println("❌ Crypto box random nonce generation failed");
                }
            } catch (Exception e) {
                System.out.println("⚠️  Crypto box random nonce failed: " + e.getMessage());
            }
            
            // Test AEAD random nonce (requires AEAD key)
            try {
                long aeadKey = AskarNative.keyGenerate("chacha20-poly1305", null, false);
                if (aeadKey != 0) {
                    byte[] aeadNonce = AskarNative.keyAeadRandomNonce(aeadKey);
                    if (aeadNonce != null && aeadNonce.length > 0) {
                        System.out.println("✅ AEAD random nonce: length=" + aeadNonce.length + 
                                         ", nonce=" + bytesToHex(Arrays.copyOf(aeadNonce, Math.min(8, aeadNonce.length))) + "...");
                    } else {
                        System.out.println("❌ AEAD random nonce generation failed");
                    }
                    AskarNative.keyFree(aeadKey);
                } else {
                    System.out.println("⚠️  Could not generate AEAD key for nonce test");
                }
            } catch (Exception e) {
                System.out.println("⚠️  AEAD random nonce failed: " + e.getMessage());
            }
            
            // Test 6: Different key exchange combinations
            System.out.println("\n6. Testing different key exchange algorithms...");
            String[] kexAlgorithms = {"p256", "secp256k1"};
            
            for (String alg : kexAlgorithms) {
                try {
                    System.out.println("   Testing key exchange with: " + alg);
                    long sk = AskarNative.keyGenerate(alg, null, false);
                    long pk = AskarNative.keyGenerate(alg, null, false);
                    
                    if (sk != 0 && pk != 0) {
                        try {
                            long derived = AskarNative.keyFromKeyExchange(alg, sk, pk);
                            if (derived != 0) {
                                String derivedAlg = AskarNative.keyGetAlgorithm(derived);
                                System.out.println("   ✅ " + alg + " key exchange succeeded, derived: " + derivedAlg);
                                AskarNative.keyFree(derived);
                            } else {
                                System.out.println("   ❌ " + alg + " key exchange failed");
                            }
                        } catch (Exception e) {
                            System.out.println("   ⚠️  " + alg + " key exchange not supported: " + e.getMessage());
                        }
                        AskarNative.keyFree(sk);
                        AskarNative.keyFree(pk);
                    } else {
                        System.out.println("   ❌ " + alg + " key generation failed");
                    }
                } catch (Exception e) {
                    System.out.println("   ⚠️  " + alg + " not supported: " + e.getMessage());
                }
            }
            
            System.out.println("\n🎉 Key Derivation Test completed!");
            
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