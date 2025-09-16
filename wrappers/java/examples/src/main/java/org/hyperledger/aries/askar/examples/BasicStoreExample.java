package org.hyperledger.aries.askar.examples;

import org.hyperledger.aries.askar.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Basic example demonstrating store operations using JNI.
 */
public class BasicStoreExample {

    public static void main(String[] args) {
        try {
            System.out.println("=== Askar JNI Basic Store Example ===");
            System.out.println("Askar version: " + StoreJNI.getVersion());

            // Create a temporary store
            String storeUri = "sqlite://test_jni.db";

            // Use a properly formatted base58 raw key for testing
            System.out.println("Using base58 raw key...");
            String rawKey = "3Mn5Pg6YkqYhzKFjpzE5LBV7LU7vGK8d5V7v2Q8N9k7K";
            System.out.println("Raw key: " + rawKey);

            // Provision a new store
            System.out.println("Provisioning store...");
            try (StoreJNI store = StoreJNI.provision(storeUri, "raw", rawKey, null, true)) {
                System.out.println("Store provisioned successfully");

                // Demonstrate basic operations
                demonstrateBasicOperations(store);

                // Note: Key operations, transactions, and profiles not yet implemented in JNI
                System.out.println("\n📝 Note: Advanced features (keys, transactions, profiles) require additional JNI implementation");
            }

            System.out.println("Example completed successfully!");

        } catch (AskarException e) {
            System.err.println("Askar error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void demonstrateBasicOperations(StoreJNI store) throws AskarException {
        System.out.println("\n=== Basic Operations ===");

        try (StoreJNI.SessionJNI session = store.session().open()) {
            System.out.println("✅ Session created successfully");
            
            // Note: Data operations (insert, count, fetch) are currently not working in JNI
            System.out.println("ℹ️  Data operations skipped (not yet working in JNI implementation):");
            System.out.println("   - insert() operations fail with INPUT error");
            System.out.println("   - count() operations not working");
            System.out.println("   - fetchAll() operations not working");
            System.out.println("   - fetch() operations not working");
            
            System.out.println("✅ Session operations (create/close) working correctly");
            System.out.println("📝 Future work: Fix sessionUpdate, sessionCount, sessionFetch functions");
        }
    }

}