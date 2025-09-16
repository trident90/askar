package org.hyperledger.aries.askar.examples;

import org.hyperledger.aries.askar.*;

/**
 * Simple test example using JNI implementation.
 * This demonstrates only the features that currently work in JNI.
 */
public class SimpleTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== Simple JNI Test (Working Features Only) ===");
            
            // Test 1: Version information
            System.out.println("1. Testing version...");
            String version = StoreJNI.getVersion();
            System.out.println("✅ Askar version: " + version);
            
            // Test 2: Store provisioning
            System.out.println("2. Testing store provisioning...");
            String storeUri = "sqlite://simple-test.db";
            String rawKey = "3Mn5Pg6YkqYhzKFjpzE5LBV7LU7vGK8d5V7v2Q8N9k7K";
            
            try (StoreJNI store = StoreJNI.provision(storeUri, "raw", rawKey, null, true)) {
                System.out.println("✅ Store provisioned successfully");
                
                // Test 3: Session creation
                System.out.println("3. Testing session creation...");
                try (StoreJNI.SessionJNI session = store.session().open()) {
                    System.out.println("✅ Session created successfully");
                    
                    System.out.println("4. Session operations completed");
                    
                    // Note: Data operations (insert, count, fetch) are currently not working in JNI
                    System.out.println("ℹ️  Data operations skipped (not yet working in JNI implementation)");
                }
                
                System.out.println("✅ All working JNI features tested successfully!");
            }
            
        } catch (AskarException e) {
            System.err.println("❌ Askar error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}