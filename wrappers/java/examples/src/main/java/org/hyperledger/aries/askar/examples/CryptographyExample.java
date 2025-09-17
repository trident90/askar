package org.hyperledger.aries.askar.examples;

import org.hyperledger.aries.askar.AskarNative;
import java.io.File;
import java.util.Arrays;

/**
 * Example demonstrating cryptographic operations using Native API.
 * Shows signing, verification, encryption, and key exchange operations.
 */
public class CryptographyExample {

    public static void main(String[] args) {
        try {
            System.out.println("=== Askar Native API Cryptography Example ===");
            
            String version = AskarNative.getVersion();
            System.out.println("✅ Askar version: " + version);

            // Test 1: Digital Signatures
            System.out.println("\n=== Digital Signature Operations ===");
            testDigitalSignatures();
            
            // Test 2: AEAD Encryption
            System.out.println("\n=== AEAD Encryption Operations ===");
            testAeadEncryption();
            
            // Test 3: Crypto Box Operations
            System.out.println("\n=== Crypto Box Operations ===");
            testCryptoBox();
            
            // Test 4: Key Derivation
            System.out.println("\n=== Key Derivation Operations ===");
            testKeyDerivation();
            
            // Test 5: Key Exchange
            System.out.println("\n=== Key Exchange Operations ===");
            testKeyExchange();
            
            System.out.println("\n🎉 Cryptography Example completed successfully!");
            System.out.println("🔐 All major cryptographic operations demonstrated!");

        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testDigitalSignatures() {
        try {
            // Generate signing keys
            long ed25519Key = AskarNative.keyGenerate("ed25519", null, false);
            long secp256k1Key = AskarNative.keyGenerate("secp256k1", null, false);
            
            if (ed25519Key != 0 && secp256k1Key != 0) {
                System.out.println("✅ Generated Ed25519 and Secp256k1 signing keys");
                
                // Test message
                byte[] message = "This is a test message for digital signatures!".getBytes();
                
                // Test Ed25519 signatures
                byte[] ed25519Signature = AskarNative.keySignMessage(ed25519Key, message, null);
                boolean ed25519Valid = AskarNative.keyVerifySignature(ed25519Key, message, ed25519Signature, null);
                System.out.println("✅ Ed25519 signature: " + (ed25519Valid ? "VALID" : "INVALID"));
                System.out.println("   Signature length: " + ed25519Signature.length + " bytes");
                
                // Test Secp256k1 signatures
                byte[] secp256k1Signature = AskarNative.keySignMessage(secp256k1Key, message, null);
                boolean secp256k1Valid = AskarNative.keyVerifySignature(secp256k1Key, message, secp256k1Signature, null);
                System.out.println("✅ Secp256k1 signature: " + (secp256k1Valid ? "VALID" : "INVALID"));
                System.out.println("   Signature length: " + secp256k1Signature.length + " bytes");
                
                // Test invalid signature
                byte[] tamperedMessage = "This is a TAMPERED message for digital signatures!".getBytes();
                boolean shouldBeFalse = AskarNative.keyVerifySignature(ed25519Key, tamperedMessage, ed25519Signature, null);
                System.out.println("✅ Tampered message verification: " + (shouldBeFalse ? "INVALID (ERROR!)" : "INVALID (CORRECT)"));
                
                AskarNative.keyFree(ed25519Key);
                AskarNative.keyFree(secp256k1Key);
            } else {
                System.out.println("⚠️  Failed to generate signing keys");
            }
        } catch (Exception e) {
            System.out.println("⚠️  Digital signature operations failed: " + e.getMessage());
        }
    }
    
    private static void testAeadEncryption() {
        try {
            // Generate AEAD encryption key
            long aeadKey = AskarNative.keyGenerate("aes256-gcm", null, false);
            
            if (aeadKey != 0) {
                System.out.println("✅ Generated AES256-GCM key");
                
                // Test data
                byte[] plaintext = "Secret message for AEAD encryption test!".getBytes();
                byte[] aad = "additional_authenticated_data".getBytes();
                
                // Generate random nonce
                byte[] nonce = AskarNative.keyAeadRandomNonce(aeadKey);
                System.out.println("✅ Generated random nonce, length: " + nonce.length + " bytes");
                
                // Encrypt
                byte[] ciphertext = AskarNative.keyAeadEncrypt(aeadKey, plaintext, nonce, aad);
                System.out.println("✅ AEAD encryption successful");
                System.out.println("   Plaintext length: " + plaintext.length + " bytes");
                System.out.println("   Ciphertext length: " + ciphertext.length + " bytes");
                
                // Extract tag (last 16 bytes for GCM)
                byte[] tag = Arrays.copyOfRange(ciphertext, ciphertext.length - 16, ciphertext.length);
                byte[] actualCiphertext = Arrays.copyOfRange(ciphertext, 0, ciphertext.length - 16);
                
                // Decrypt
                byte[] decrypted = AskarNative.keyAeadDecrypt(aeadKey, actualCiphertext, nonce, tag, aad);
                String decryptedText = new String(decrypted);
                System.out.println("✅ AEAD decryption successful: " + decryptedText);
                
                // Verify original plaintext matches decrypted
                boolean matches = Arrays.equals(plaintext, decrypted);
                System.out.println("✅ Plaintext integrity: " + (matches ? "VERIFIED" : "FAILED"));
                
                AskarNative.keyFree(aeadKey);
            } else {
                System.out.println("⚠️  Failed to generate AEAD key");
            }
        } catch (Exception e) {
            System.out.println("⚠️  AEAD encryption operations failed: " + e.getMessage());
        }
    }
    
    private static void testCryptoBox() {
        try {
            // Generate X25519 keys for crypto box
            long aliceKey = AskarNative.keyGenerate("x25519", null, false);
            long bobKey = AskarNative.keyGenerate("x25519", null, false);
            
            if (aliceKey != 0 && bobKey != 0) {
                System.out.println("✅ Generated Alice's and Bob's X25519 keys");
                
                // Test message
                byte[] message = "Secret message from Alice to Bob!".getBytes();
                
                // Generate random nonce
                byte[] nonce = AskarNative.keyCryptoBoxRandomNonce();
                System.out.println("✅ Generated crypto box nonce, length: " + nonce.length + " bytes");
                
                // Alice encrypts message for Bob
                byte[] encrypted = AskarNative.keyCryptoBox(bobKey, aliceKey, message, nonce);
                System.out.println("✅ Alice encrypted message for Bob");
                System.out.println("   Original length: " + message.length + " bytes");
                System.out.println("   Encrypted length: " + encrypted.length + " bytes");
                
                // Bob decrypts message from Alice
                byte[] decrypted = AskarNative.keyCryptoBoxOpen(bobKey, aliceKey, encrypted, nonce);
                String decryptedText = new String(decrypted);
                System.out.println("✅ Bob decrypted message from Alice: " + decryptedText);
                
                // Verify message integrity
                boolean matches = Arrays.equals(message, decrypted);
                System.out.println("✅ Message integrity: " + (matches ? "VERIFIED" : "FAILED"));
                
                AskarNative.keyFree(aliceKey);
                AskarNative.keyFree(bobKey);
            } else {
                System.out.println("⚠️  Failed to generate crypto box keys");
            }
        } catch (Exception e) {
            System.out.println("⚠️  Crypto box operations failed: " + e.getMessage());
        }
    }
    
    private static void testKeyDerivation() {
        try {
            // Generate seed for key derivation
            byte[] seed = "test_seed_for_key_derivation_example_32".getBytes(); // 32 bytes
            
            // Derive keys from seed
            long derivedEd25519 = AskarNative.keyFromSeed("ed25519", seed, null);
            long derivedX25519 = AskarNative.keyFromSeed("x25519", seed, null);
            
            if (derivedEd25519 != 0 && derivedX25519 != 0) {
                System.out.println("✅ Derived Ed25519 and X25519 keys from seed");
                
                // Get key information
                String ed25519Alg = AskarNative.keyGetAlgorithm(derivedEd25519);
                String x25519Alg = AskarNative.keyGetAlgorithm(derivedX25519);
                byte[] ed25519Public = AskarNative.keyGetPublicBytes(derivedEd25519);
                byte[] x25519Public = AskarNative.keyGetPublicBytes(derivedX25519);
                
                System.out.println("✅ Derived key info:");
                System.out.println("   " + ed25519Alg + " public key: " + bytesToHex(ed25519Public));
                System.out.println("   " + x25519Alg + " public key: " + bytesToHex(x25519Public));
                
                // Test key conversion
                long convertedKey = AskarNative.keyConvert(derivedEd25519, "ed25519");
                if (convertedKey != 0) {
                    System.out.println("✅ Key conversion successful");
                    AskarNative.keyFree(convertedKey);
                } else {
                    System.out.println("⚠️  Key conversion failed");
                }
                
                AskarNative.keyFree(derivedEd25519);
                AskarNative.keyFree(derivedX25519);
            } else {
                System.out.println("⚠️  Key derivation from seed failed");
            }
        } catch (Exception e) {
            System.out.println("⚠️  Key derivation operations failed: " + e.getMessage());
        }
    }
    
    private static void testKeyExchange() {
        try {
            // Generate ephemeral and recipient keys
            long ephemeralKey = AskarNative.keyGenerate("x25519", null, false);
            long recipientKey = AskarNative.keyGenerate("x25519", null, false);
            
            if (ephemeralKey != 0 && recipientKey != 0) {
                System.out.println("✅ Generated ephemeral and recipient keys");
                
                // ECDH-ES key derivation
                byte[] algorithmId = "A256GCM".getBytes();
                byte[] apu = "Alice".getBytes();
                byte[] apv = "Bob".getBytes();
                
                long derivedKey = AskarNative.keyDeriveEcdhEs(
                    "aes256-gcm", ephemeralKey, recipientKey, 
                    algorithmId, apu, apv, false
                );
                
                if (derivedKey != 0) {
                    System.out.println("✅ ECDH-ES key derivation successful");
                    
                    String algorithm = AskarNative.keyGetAlgorithm(derivedKey);
                    System.out.println("   Derived key algorithm: " + algorithm);
                    
                    AskarNative.keyFree(derivedKey);
                } else {
                    System.out.println("⚠️  ECDH-ES key derivation failed");
                }
                
                // Key exchange using keyFromKeyExchange
                long exchangedKey = AskarNative.keyFromKeyExchange("aes256-gcm", ephemeralKey, recipientKey);
                if (exchangedKey != 0) {
                    System.out.println("✅ Key exchange successful");
                    String exchangeAlgorithm = AskarNative.keyGetAlgorithm(exchangedKey);
                    System.out.println("   Exchanged key algorithm: " + exchangeAlgorithm);
                    AskarNative.keyFree(exchangedKey);
                } else {
                    System.out.println("⚠️  Key exchange failed");
                }
                
                AskarNative.keyFree(ephemeralKey);
                AskarNative.keyFree(recipientKey);
            } else {
                System.out.println("⚠️  Failed to generate key exchange keys");
            }
        } catch (Exception e) {
            System.out.println("⚠️  Key exchange operations failed: " + e.getMessage());
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