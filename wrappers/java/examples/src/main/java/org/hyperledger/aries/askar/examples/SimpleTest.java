package org.hyperledger.aries.askar.examples;

import org.hyperledger.aries.askar.*;

public class SimpleTest {
    public static void main(String[] args) {
        try {
            LibraryLoader.setMaxLogLevel(3);
            System.out.println("Askar version: " + LibraryLoader.getVersion());

            String storeUri = "sqlite://simple-test.db";
            String rawKey = "3Mn5Pg6YkqYhzKFjpzE5LBV7LU7vGK8d5V7v2Q8N9k7K";

            System.out.println("Creating store...");
            try (Store store = Store.provision(storeUri, "raw", rawKey, null, true)) {
                System.out.println("Store created successfully");

                System.out.println("Opening session...");
                try (Session session = store.session().open()) {
                    System.out.println("Session opened successfully");

                    // Test with minimal data - just a simple string
                    System.out.println("Inserting minimal data...");
                    // Try with empty map instead of null for tags
                    java.util.Map<String, Object> emptyTags = new java.util.HashMap<>();
                    session.insert("test", "item1", "hello".getBytes(), emptyTags, null);
                    System.out.println("Data inserted successfully");

                    System.out.println("Test completed successfully!");
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}