package org.hyperledger.aries.askar.examples;

import org.hyperledger.aries.askar.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Basic example demonstrating store operations.
 */
public class BasicStoreExample {

    public static void main(String[] args) {
        try {
            // Initialize library and set log level
            LibraryLoader.setMaxLogLevel(3); // INFO level

            System.out.println("Askar version: " + LibraryLoader.getVersion());

            // Create a temporary store
            String storeUri = "sqlite://test.db";

            // Use a properly formatted base58 raw key for testing
            System.out.println("Using base58 raw key...");
            String rawKey = "3Mn5Pg6YkqYhzKFjpzE5LBV7LU7vGK8d5V7v2Q8N9k7K";
            System.out.println("Raw key: " + rawKey);

            // Provision a new store
            System.out.println("Provisioning store...");
            try (Store store = Store.provision(storeUri, "raw", rawKey, null, true)) {
                System.out.println("Store provisioned successfully");

                // Demonstrate basic operations
                demonstrateBasicOperations(store);

                // Demonstrate key operations
                demonstrateKeyOperations(store);

                // Demonstrate transactions
                demonstrateTransactions(store);

                // Demonstrate profiles
                demonstrateProfiles(store);
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

    private static void demonstrateBasicOperations(Store store) throws AskarException {
        System.out.println("\n=== Basic Operations ===");

        try (Session session = store.session().open()) {
            // Insert some data
            Map<String, Object> tags = new HashMap<>();
            tags.put("type", "credential");
            tags.put("version", 1);

            session.insert("credentials", "cred1",
                    "{\"id\": \"credential-1\", \"type\": \"VerifiableCredential\"}".getBytes(),
                    tags, null);

            session.insert("credentials", "cred2",
                    "{\"id\": \"credential-2\", \"type\": \"VerifiableCredential\"}".getBytes(),
                    tags, null);

            System.out.println("Inserted 2 credentials");

            // Count entries
            int count = session.count("credentials", null);
            System.out.println("Total credentials: " + count);

            // Fetch specific entry
            Entry entry = session.fetch("credentials", "cred1", false);
            if (entry != null) {
                System.out.println("Fetched credential: " + entry.getValueString());
                System.out.println("Tags: " + entry.getTags());
            }

            // Fetch all entries in category
            List<Entry> entries = session.fetchAll("credentials", null, null, null, false, false);
            System.out.println("Found " + entries.size() + " credentials:");
            for (Entry e : entries) {
                System.out.println("  - " + e.getName() + ": " + e.getValueString());
            }

            // Update an entry
            session.replace("credentials", "cred1",
                    "{\"id\": \"credential-1-updated\", \"type\": \"VerifiableCredential\"}".getBytes(),
                    tags, null);
            System.out.println("Updated credential cred1");

            // Remove an entry
            session.remove("credentials", "cred2");
            System.out.println("Removed credential cred2");
        }
    }

    private static void demonstrateKeyOperations(Store store) throws AskarException {
        System.out.println("\n=== Key Operations ===");

        try (Session session = store.session().open()) {
            // Generate a signing key
            try (Key signingKey = Key.generate(KeyAlgorithm.ED25519, false)) {
                System.out.println("Generated Ed25519 signing key");
                System.out.println("Algorithm: " + signingKey.getAlgorithm());
                System.out.println("Ephemeral: " + signingKey.isEphemeral());

                // Store the key
                Map<String, Object> keyTags = new HashMap<>();
                keyTags.put("purpose", "signing");
                keyTags.put("curve", "ed25519");

                session.insertKey("my-signing-key", signingKey, "Primary signing key", keyTags, null);
                System.out.println("Stored signing key");

                // Sign a message
                byte[] message = "Hello, Askar!".getBytes();
                byte[] signature = signingKey.signMessage(message, null);
                System.out.println("Signed message, signature length: " + signature.length);

                // Verify signature
                boolean isValid = signingKey.verifySignature(message, signature, null);
                System.out.println("Signature valid: " + isValid);

                // Convert to X25519 for key exchange
                try (Key exchangeKey = signingKey.convert(KeyAlgorithm.X25519)) {
                    System.out.println("Converted to X25519 for key exchange");

                    session.insertKey("my-exchange-key", exchangeKey, "Key exchange key", keyTags, null);
                    System.out.println("Stored exchange key");
                }
            }

            // Retrieve the stored key
            KeyEntry keyEntry = session.fetchKey("my-signing-key", false);
            if (keyEntry != null) {
                System.out.println("Retrieved key: " + keyEntry.getName());
                System.out.println("Metadata: " + keyEntry.getMetadata());
                System.out.println("Algorithm: " + keyEntry.getAlgorithm());
                System.out.println("Tags: " + keyEntry.getTags());

                try (Key retrievedKey = keyEntry.getKey()) {
                    // Use the retrieved key
                    byte[] publicBytes = retrievedKey.getPublicBytes();
                    System.out.println("Public key length: " + publicBytes.length);
                }
            }
        }
    }

    private static void demonstrateTransactions(Store store) throws AskarException {
        System.out.println("\n=== Transactions ===");

        // Demonstrate rollback
        try (Session txn = store.transaction().open()) {
            txn.insert("temp", "data1", "temporary data".getBytes(), null, null);
            System.out.println("Inserted data in transaction");

            // Rollback the transaction
            txn.rollback();
            System.out.println("Transaction rolled back");
        }

        // Verify data was not persisted
        try (Session session = store.session().open()) {
            Entry entry = session.fetch("temp", "data1", false);
            System.out.println("Data after rollback: " + (entry == null ? "null" : "exists"));
        }

        // Demonstrate commit
        try (Session txn = store.transaction().open()) {
            txn.insert("persistent", "data1", "persistent data".getBytes(), null, null);
            System.out.println("Inserted data in transaction");

            // Commit the transaction
            txn.commit();
            System.out.println("Transaction committed");
        }

        // Verify data was persisted
        try (Session session = store.session().open()) {
            Entry entry = session.fetch("persistent", "data1", false);
            System.out.println("Data after commit: " + (entry != null ? entry.getValueString() : "null"));
        }
    }

    private static void demonstrateProfiles(Store store) throws AskarException {
        System.out.println("\n=== Profiles ===");

        // Create additional profiles
        String profile1 = store.createProfile("test-profile-1");
        String profile2 = store.createProfile(null); // Auto-generated name
        System.out.println("Created profiles: " + profile1 + ", " + profile2);

        // List all profiles
        List<String> profiles = store.listProfiles();
        System.out.println("All profiles: " + profiles);

        // Store data in specific profile
        try (Session session = store.session(profile1).open()) {
            session.insert("profile-data", "item1", "data from profile 1".getBytes(), null, null);
            System.out.println("Stored data in " + profile1);
        }

        // Try to access from default profile (should not see the data)
        try (Session session = store.session().open()) {
            Entry entry = session.fetch("profile-data", "item1", false);
            System.out.println("Data visible from default profile: " + (entry != null));
        }

        // Access from the correct profile
        try (Session session = store.session(profile1).open()) {
            Entry entry = session.fetch("profile-data", "item1", false);
            System.out.println("Data from " + profile1 + ": " +
                    (entry != null ? entry.getValueString() : "null"));
        }

        // Remove a profile
        boolean removed = store.removeProfile(profile2);
        System.out.println("Removed " + profile2 + ": " + removed);

        profiles = store.listProfiles();
        System.out.println("Profiles after removal: " + profiles);
    }
}