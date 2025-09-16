package org.hyperledger.aries.askar.examples;

import org.hyperledger.aries.askar.*;

/**
 * Example demonstrating basic JNI operations (cryptography features limited in current JNI implementation).
 */
public class CryptographyExample {

    public static void main(String[] args) {
        try {
            System.out.println("=== Askar JNI Example (Limited Crypto Support) ===\n");
            System.out.println("Askar JNI version: " + StoreJNI.getVersion());

            // Note: Full cryptography features require Key class JNI implementation
            // For now, demonstrate basic store operations that work with JNI
            demonstrateBasicStoreOperations();

            System.out.println("JNI example completed successfully!");
            System.out.println("Note: Full cryptography examples require additional JNI implementation");

        } catch (AskarException e) {
            System.err.println("Askar error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void demonstrateBasicStoreOperations() throws AskarException {
        System.out.println("=== Basic Store Operations via JNI ===");

        String storeUri = "sqlite://crypto_jni_test.db";
        String rawKey = "3Mn5Pg6YkqYhzKFjpzE5LBV7LU7vGK8d5V7v2Q8N9k7K";

        try (StoreJNI store = StoreJNI.provision(storeUri, "raw", rawKey, null, true)) {
            System.out.println("✅ Store provisioned successfully");

            try (StoreJNI.SessionJNI session = store.session().open()) {
                System.out.println("✅ Session created successfully");

                // Note: Data operations (insert, count, fetch) are currently not working in JNI
                System.out.println("ℹ️  Cryptographic data operations skipped (not yet working in JNI implementation):");
                System.out.println("   - insert() operations fail with INPUT error");
                System.out.println("   - fetchAll() operations not working");
                System.out.println("   - Key generation/management not implemented");
                System.out.println("   - Cryptographic operations not available");
                
                System.out.println("✅ Session operations (create/close) working correctly");
                System.out.println("📝 Future work: Implement Key class JNI bindings for full crypto support");
            }
        }
    }
}