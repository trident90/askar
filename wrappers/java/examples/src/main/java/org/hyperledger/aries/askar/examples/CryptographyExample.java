package org.hyperledger.aries.askar.examples;

import org.hyperledger.aries.askar.*;

/**
 * Example demonstrating cryptographic operations.
 */
public class CryptographyExample {

    public static void main(String[] args) {
        try {
            LibraryLoader.setMaxLogLevel(3);
            System.out.println("=== Cryptography Examples ===\n");

            demonstrateSigningKeys();
            demonstrateKeyDerivation();
            demonstrateKeyFormats();
            demonstrateKeyConversion();

            System.out.println("Cryptography examples completed successfully!");

        } catch (AskarException e) {
            System.err.println("Askar error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void demonstrateSigningKeys() throws AskarException {
        System.out.println("=== Digital Signatures ===");

        // Generate Ed25519 signing key
        try (Key ed25519Key = Key.generate(KeyAlgorithm.ED25519, false)) {
            System.out.println("Generated Ed25519 key");

            byte[] message = "This is a test message for signing".getBytes();

            // Sign the message
            byte[] signature = ed25519Key.signMessage(message, null);
            System.out.println("Message signed, signature length: " + signature.length);

            // Verify the signature
            boolean isValid = ed25519Key.verifySignature(message, signature, null);
            System.out.println("Signature valid: " + isValid);

            // Test with tampered message
            byte[] tamperedMessage = "This is a tampered message for signing".getBytes();
            boolean isInvalid = ed25519Key.verifySignature(tamperedMessage, signature, null);
            System.out.println("Tampered message signature valid: " + isInvalid);

            // Extract public key for verification-only operations
            byte[] publicBytes = ed25519Key.getPublicBytes();
            try (Key publicKey = Key.fromPublicBytes(KeyAlgorithm.ED25519, publicBytes)) {
                boolean verifyWithPublic = publicKey.verifySignature(message, signature, null);
                System.out.println("Verification with public key: " + verifyWithPublic);
            }
        }

        // Generate secp256k1 signing key
        try (Key secp256k1Key = Key.generate(KeyAlgorithm.ES256K, false)) {
            System.out.println("\nGenerated secp256k1 key");

            byte[] message = "Secp256k1 signature test".getBytes();
            byte[] signature = secp256k1Key.signMessage(message, null);
            boolean isValid = secp256k1Key.verifySignature(message, signature, null);
            System.out.println("Secp256k1 signature valid: " + isValid);
        }

        System.out.println();
    }

    private static void demonstrateKeyDerivation() throws AskarException {
        System.out.println("=== Key Derivation ===");

        // Create deterministic keys from seed
        byte[] seed = new byte[32];
        for (int i = 0; i < seed.length; i++) {
            seed[i] = (byte) (i * 7 % 256); // Simple pattern
        }

        // Derive multiple keys from same seed
        try (Key key1 = Key.fromSeed(KeyAlgorithm.ED25519, seed, SeedMethod.BLAKE2B);
             Key key2 = Key.fromSeed(KeyAlgorithm.ED25519, seed, SeedMethod.BLAKE2B);
             Key key3 = Key.fromSeed(KeyAlgorithm.ED25519, seed, SeedMethod.RAW)) {

            byte[] pubBytes1 = key1.getPublicBytes();
            byte[] pubBytes2 = key2.getPublicBytes();
            byte[] pubBytes3 = key3.getPublicBytes();

            // Keys derived with same method should be identical
            boolean sameKeys = java.util.Arrays.equals(pubBytes1, pubBytes2);
            System.out.println("Keys from same seed/method are identical: " + sameKeys);

            // Keys derived with different methods should be different
            boolean differentMethods = !java.util.Arrays.equals(pubBytes1, pubBytes3);
            System.out.println("Keys from different methods are different: " + differentMethods);

            System.out.println("BLAKE2B derived key: " + bytesToHex(pubBytes1, 8));
            System.out.println("RAW derived key: " + bytesToHex(pubBytes3, 8));
        }

        System.out.println();
    }

    private static void demonstrateKeyFormats() throws AskarException {
        System.out.println("=== Key Formats ===");

        try (Key key = Key.generate(KeyAlgorithm.ED25519, false)) {
            // Raw bytes format
            byte[] publicBytes = key.getPublicBytes();
            byte[] secretBytes = key.getSecretBytes();
            System.out.println("Public key bytes (" + publicBytes.length + "): " + bytesToHex(publicBytes, 8));
            System.out.println("Secret key bytes (" + secretBytes.length + "): " + bytesToHex(secretBytes, 8));

            // JWK format
            String jwkPublic = key.getJwkPublic(null);
            System.out.println("JWK public key: " + jwkPublic.substring(0, Math.min(jwkPublic.length(), 80)) + "...");

            byte[] jwkSecret = key.getJwkSecret();
            String jwkSecretStr = new String(jwkSecret);
            System.out.println("JWK secret key: " + jwkSecretStr.substring(0, Math.min(jwkSecretStr.length(), 80)) + "...");

            // Round-trip test: create key from JWK
            try (Key keyFromJwk = Key.fromJwk(jwkSecretStr)) {
                byte[] originalPubBytes = key.getPublicBytes();
                byte[] jwkPubBytes = keyFromJwk.getPublicBytes();
                boolean roundTripSuccess = java.util.Arrays.equals(originalPubBytes, jwkPubBytes);
                System.out.println("JWK round-trip successful: " + roundTripSuccess);
            }

            // Round-trip test: create key from secret bytes
            try (Key keyFromSecret = Key.fromSecretBytes(KeyAlgorithm.ED25519, secretBytes)) {
                byte[] originalPubBytes = key.getPublicBytes();
                byte[] secretPubBytes = keyFromSecret.getPublicBytes();
                boolean secretRoundTrip = java.util.Arrays.equals(originalPubBytes, secretPubBytes);
                System.out.println("Secret bytes round-trip successful: " + secretRoundTrip);
            }
        }

        System.out.println();
    }

    private static void demonstrateKeyConversion() throws AskarException {
        System.out.println("=== Key Conversion ===");

        // Start with Ed25519 signing key
        try (Key ed25519Key = Key.generate(KeyAlgorithm.ED25519, false)) {
            System.out.println("Generated Ed25519 signing key");

            // Convert to X25519 for key exchange
            try (Key x25519Key = ed25519Key.convert(KeyAlgorithm.X25519)) {
                System.out.println("Converted to X25519 key exchange key");
                System.out.println("Ed25519 algorithm: " + ed25519Key.getAlgorithm());
                System.out.println("X25519 algorithm: " + x25519Key.getAlgorithm());

                // Show that public keys are different but mathematically related
                byte[] ed25519PubBytes = ed25519Key.getPublicBytes();
                byte[] x25519PubBytes = x25519Key.getPublicBytes();

                System.out.println("Ed25519 public key: " + bytesToHex(ed25519PubBytes, 8));
                System.out.println("X25519 public key: " + bytesToHex(x25519PubBytes, 8));
                System.out.println("Keys are different: " + !java.util.Arrays.equals(ed25519PubBytes, x25519PubBytes));

                // Test signing with Ed25519
                byte[] message = "Test message".getBytes();
                byte[] signature = ed25519Key.signMessage(message, null);
                boolean signatureValid = ed25519Key.verifySignature(message, signature, null);
                System.out.println("Ed25519 signature valid: " + signatureValid);

                // X25519 keys cannot sign (would throw exception)
                System.out.println("X25519 is for key exchange, not signing");
            }
        }

        // Demonstrate secp256r1 curve
        try (Key p256Key = Key.generate(KeyAlgorithm.ES256, false)) {
            System.out.println("\nGenerated P-256 (secp256r1) key");
            System.out.println("Algorithm: " + p256Key.getAlgorithm());

            byte[] message = "P-256 test message".getBytes();
            byte[] signature = p256Key.signMessage(message, null);
            boolean valid = p256Key.verifySignature(message, signature, null);
            System.out.println("P-256 signature valid: " + valid);
        }

        System.out.println();
    }

    private static String bytesToHex(byte[] bytes, int maxLength) {
        StringBuilder sb = new StringBuilder();
        int length = Math.min(bytes.length, maxLength);
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02x", bytes[i]));
        }
        if (bytes.length > maxLength) {
            sb.append("...");
        }
        return sb.toString();
    }
}