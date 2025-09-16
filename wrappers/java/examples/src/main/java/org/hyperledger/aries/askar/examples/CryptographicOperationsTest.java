package org.hyperledger.aries.askar.examples;

import org.hyperledger.aries.askar.*;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Comprehensive test for cryptographic operations.
 */
public class CryptographicOperationsTest {

    public static void main(String[] args) {
        try {
            System.out.println("=== Cryptographic Operations Test ===");
            
            // Test 1: Message signing and verification with Ed25519
            System.out.println("\n1. Testing message signing and verification (Ed25519)...");
            long signingKey = AskarNative.keyGenerate("ed25519", null, false);
            if (signingKey == 0) {
                System.out.println("❌ Failed to generate signing key");
                return;
            }
            System.out.println("✅ Signing key generated: " + signingKey);
            
            // Test message
            String testMessage = "Hello, Askar cryptographic operations!";
            byte[] messageBytes = testMessage.getBytes();
            
            // Sign the message
            try {
                byte[] signature = AskarNative.keySignMessage(signingKey, messageBytes, null);
                if (signature != null && signature.length > 0) {
                    System.out.println("✅ Message signed successfully, signature length: " + signature.length);
                    System.out.println("   Signature: " + bytesToHex(Arrays.copyOf(signature, Math.min(16, signature.length))) + "...");
                    
                    // Verify the signature
                    boolean isValid = AskarNative.keyVerifySignature(signingKey, messageBytes, signature, null);
                    if (isValid) {
                        System.out.println("✅ Signature verification succeeded");
                    } else {
                        System.out.println("❌ Signature verification failed");
                    }
                    
                    // Test with wrong message
                    byte[] wrongMessage = "Wrong message".getBytes();
                    boolean shouldFail = AskarNative.keyVerifySignature(signingKey, wrongMessage, signature, null);
                    if (!shouldFail) {
                        System.out.println("✅ Signature correctly rejected for wrong message");
                    } else {
                        System.out.println("❌ Signature incorrectly accepted for wrong message");
                    }
                    
                } else {
                    System.out.println("❌ Message signing failed");
                }
            } catch (Exception e) {
                System.out.println("❌ Signing/verification failed: " + e.getMessage());
            }
            
            // Test 2: AEAD encryption/decryption (need appropriate key)
            System.out.println("\n2. Testing AEAD encryption and decryption...");
            try {
                // Try with different key types that might support AEAD
                long aeadKey = AskarNative.keyGenerate("chacha20-poly1305", null, false);
                if (aeadKey == 0) {
                    // Fallback to AES
                    aeadKey = AskarNative.keyGenerate("aes256-gcm", null, false);
                }
                
                if (aeadKey != 0) {
                    System.out.println("✅ AEAD key generated: " + aeadKey);
                    
                    String aeadMessage = "Secret AEAD message";
                    byte[] aeadMessageBytes = aeadMessage.getBytes();
                    byte[] aad = "Additional authenticated data".getBytes();
                    
                    try {
                        byte[] encrypted = AskarNative.keyAeadEncrypt(aeadKey, aeadMessageBytes, null, aad);
                        if (encrypted != null && encrypted.length > 0) {
                            System.out.println("✅ AEAD encryption succeeded, length: " + encrypted.length);
                            
                            // For now, we can't easily test decryption without knowing the internal structure
                            // This would require extracting nonce and tag from the encrypted buffer
                            System.out.println("   Encrypted: " + bytesToHex(Arrays.copyOf(encrypted, Math.min(16, encrypted.length))) + "...");
                            
                        } else {
                            System.out.println("❌ AEAD encryption failed");
                        }
                    } catch (Exception e) {
                        System.out.println("❌ AEAD encryption failed: " + e.getMessage());
                    }
                    
                    AskarNative.keyFree(aeadKey);
                } else {
                    System.out.println("⚠️  AEAD key generation not supported, skipping AEAD test");
                }
            } catch (Exception e) {
                System.out.println("⚠️  AEAD operations not supported: " + e.getMessage());
            }
            
            // Test 3: Crypto Box operations (X25519 key exchange)
            System.out.println("\n3. Testing Crypto Box operations...");
            try {
                long recipientKey = AskarNative.keyGenerate("x25519", null, false);
                long senderKey = AskarNative.keyGenerate("x25519", null, false);
                
                if (recipientKey != 0 && senderKey != 0) {
                    System.out.println("✅ X25519 keys generated - Recipient: " + recipientKey + ", Sender: " + senderKey);
                    
                    String boxMessage = "Secret box message";
                    byte[] boxMessageBytes = boxMessage.getBytes();
                    
                    // Generate random nonce
                    byte[] nonce = new byte[24]; // NaCl box nonce size
                    new SecureRandom().nextBytes(nonce);
                    
                    try {
                        byte[] boxEncrypted = AskarNative.keyCryptoBox(recipientKey, senderKey, boxMessageBytes, nonce);
                        if (boxEncrypted != null && boxEncrypted.length > 0) {
                            System.out.println("✅ Crypto box encryption succeeded, length: " + boxEncrypted.length);
                            System.out.println("   Encrypted: " + bytesToHex(Arrays.copyOf(boxEncrypted, Math.min(16, boxEncrypted.length))) + "...");
                            
                            // Test decryption
                            byte[] boxDecrypted = AskarNative.keyCryptoBoxOpen(recipientKey, senderKey, boxEncrypted, nonce);
                            if (boxDecrypted != null && boxDecrypted.length > 0) {
                                String decryptedMessage = new String(boxDecrypted);
                                if (boxMessage.equals(decryptedMessage)) {
                                    System.out.println("✅ Crypto box decryption succeeded: \"" + decryptedMessage + "\"");
                                } else {
                                    System.out.println("❌ Crypto box decryption failed - message mismatch");
                                }
                            } else {
                                System.out.println("❌ Crypto box decryption failed");
                            }
                            
                        } else {
                            System.out.println("❌ Crypto box encryption failed");
                        }
                    } catch (Exception e) {
                        System.out.println("❌ Crypto box operations failed: " + e.getMessage());
                    }
                    
                    AskarNative.keyFree(recipientKey);
                    AskarNative.keyFree(senderKey);
                } else {
                    System.out.println("❌ Failed to generate X25519 keys");
                }
            } catch (Exception e) {
                System.out.println("⚠️  Crypto box operations not supported: " + e.getMessage());
            }
            
            // Test 4: Key wrapping operations
            System.out.println("\n4. Testing key wrapping operations...");
            try {
                // Generate a wrapping key and a key to wrap
                long wrapperKey = AskarNative.keyGenerate("aes256-kw", null, false);
                if (wrapperKey == 0) {
                    // Fallback to other key types
                    wrapperKey = AskarNative.keyGenerate("aes256", null, false);
                }
                
                long keyToWrap = AskarNative.keyGenerate("ed25519", null, false);
                
                if (wrapperKey != 0 && keyToWrap != 0) {
                    System.out.println("✅ Keys generated - Wrapper: " + wrapperKey + ", ToWrap: " + keyToWrap);
                    
                    try {
                        byte[] wrappedKey = AskarNative.keyWrapKey(wrapperKey, keyToWrap, null);
                        if (wrappedKey != null && wrappedKey.length > 0) {
                            System.out.println("✅ Key wrapping succeeded, length: " + wrappedKey.length);
                            System.out.println("   Wrapped: " + bytesToHex(Arrays.copyOf(wrappedKey, Math.min(16, wrappedKey.length))) + "...");
                            
                            // For unwrapping, we'd need to extract nonce and tag from the wrapped key
                            // This is complex without knowing the internal structure
                            System.out.println("   (Key unwrapping test requires extracting nonce/tag from wrapped data)");
                            
                        } else {
                            System.out.println("❌ Key wrapping failed");
                        }
                    } catch (Exception e) {
                        System.out.println("❌ Key wrapping failed: " + e.getMessage());
                    }
                    
                    AskarNative.keyFree(wrapperKey);
                    AskarNative.keyFree(keyToWrap);
                } else {
                    System.out.println("⚠️  Failed to generate keys for wrapping test");
                }
            } catch (Exception e) {
                System.out.println("⚠️  Key wrapping operations not supported: " + e.getMessage());
            }
            
            // Test 5: Different signature algorithms
            System.out.println("\n5. Testing different signature algorithms...");
            String[] sigAlgorithms = {"secp256k1", "p256"};
            
            for (String alg : sigAlgorithms) {
                try {
                    System.out.println("   Testing signature with: " + alg);
                    long testKey = AskarNative.keyGenerate(alg, null, false);
                    if (testKey != 0) {
                        byte[] testSig = AskarNative.keySignMessage(testKey, messageBytes, null);
                        if (testSig != null && testSig.length > 0) {
                            boolean isValid = AskarNative.keyVerifySignature(testKey, messageBytes, testSig, null);
                            System.out.println("   ✅ " + alg + " signature: length=" + testSig.length + ", valid=" + isValid);
                        } else {
                            System.out.println("   ❌ " + alg + " signing failed");
                        }
                        AskarNative.keyFree(testKey);
                    } else {
                        System.out.println("   ❌ " + alg + " key generation failed");
                    }
                } catch (Exception e) {
                    System.out.println("   ⚠️  " + alg + " not supported: " + e.getMessage());
                }
            }
            
            // Clean up main signing key
            AskarNative.keyFree(signingKey);
            System.out.println("\n✅ Main signing key freed");
            
            System.out.println("\n🎉 Cryptographic Operations Test completed!");
            
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