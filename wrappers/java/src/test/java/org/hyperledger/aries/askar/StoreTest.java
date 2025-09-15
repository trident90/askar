package org.hyperledger.aries.askar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Store functionality.
 */
public class StoreTest {

    @TempDir
    Path tempDir;

    private String storeUri;

    @BeforeEach
    void setUp() {
        storeUri = "sqlite://" + tempDir.resolve("test.db").toString();
    }

    @Test
    void testProvisionAndOpen() throws AskarException {
        // Provision a new store
        Store store = Store.provision(storeUri, "raw", null, null, false);
        assertNotNull(store);
        assertEquals(storeUri, store.getUri());

        // Close and reopen
        store.close();

        Store reopened = Store.open(storeUri, "raw", null, null);
        assertNotNull(reopened);
        assertEquals(storeUri, reopened.getUri());
        reopened.close();
    }

    @Test
    void testBasicOperations() throws AskarException {
        try (Store store = Store.provision(storeUri, "raw", null, null, false)) {
            // Test session operations
            try (Session session = store.session().open()) {
                // Insert entry
                session.insert("category1", "name1", "value1".getBytes(), null, null);

                // Fetch entry
                Entry entry = session.fetch("category1", "name1", false);
                assertNotNull(entry);
                assertEquals("category1", entry.getCategory());
                assertEquals("name1", entry.getName());
                assertEquals("value1", entry.getValueString());

                // Count entries
                int count = session.count("category1", null);
                assertEquals(1, count);

                // Update entry
                session.replace("category1", "name1", "value2".getBytes(), null, null);

                // Fetch updated entry
                entry = session.fetch("category1", "name1", false);
                assertNotNull(entry);
                assertEquals("value2", entry.getValueString());

                // Remove entry
                session.remove("category1", "name1");

                // Verify removal
                entry = session.fetch("category1", "name1", false);
                assertNull(entry);
            }
        }
    }

    @Test
    void testWithTags() throws AskarException {
        try (Store store = Store.provision(storeUri, "raw", null, null, false)) {
            try (Session session = store.session().open()) {
                // Insert entry with tags
                Map<String, Object> tags = new HashMap<>();
                tags.put("type", "credential");
                tags.put("version", 1);

                session.insert("category1", "name1", "value1".getBytes(), tags, null);

                // Fetch entry and verify tags
                Entry entry = session.fetch("category1", "name1", false);
                assertNotNull(entry);
                assertEquals("value1", entry.getValueString());

                Map<String, Object> retrievedTags = entry.getTags();
                assertNotNull(retrievedTags);
                assertEquals("credential", retrievedTags.get("type"));
                assertEquals(1, retrievedTags.get("version"));
            }
        }
    }

    @Test
    void testTransaction() throws AskarException {
        try (Store store = Store.provision(storeUri, "raw", null, null, false)) {
            // Test transaction rollback
            try (Session session = store.transaction().open()) {
                session.insert("category1", "name1", "value1".getBytes(), null, null);
                session.rollback();
            }

            // Verify entry was not persisted
            try (Session session = store.session().open()) {
                Entry entry = session.fetch("category1", "name1", false);
                assertNull(entry);
            }

            // Test transaction commit
            try (Session session = store.transaction().open()) {
                session.insert("category1", "name1", "value1".getBytes(), null, null);
                session.commit();
            }

            // Verify entry was persisted
            try (Session session = store.session().open()) {
                Entry entry = session.fetch("category1", "name1", false);
                assertNotNull(entry);
                assertEquals("value1", entry.getValueString());
            }
        }
    }

    @Test
    void testKeyOperations() throws AskarException {
        try (Store store = Store.provision(storeUri, "raw", null, null, false)) {
            try (Session session = store.session().open()) {
                // Generate a key
                Key key = Key.generate(KeyAlgorithm.ED25519, false);
                assertNotNull(key);
                assertEquals(KeyAlgorithm.ED25519, key.getAlgorithm());

                // Insert key
                String keyName = "test-key";
                session.insertKey(keyName, key, "Test key", null, null);

                // Fetch key
                KeyEntry keyEntry = session.fetchKey(keyName, false);
                assertNotNull(keyEntry);
                assertEquals(keyName, keyEntry.getName());
                assertEquals("ed25519", keyEntry.getAlgorithm());
                assertEquals("Test key", keyEntry.getMetadata());

                Key retrievedKey = keyEntry.getKey();
                assertNotNull(retrievedKey);
                assertEquals(KeyAlgorithm.ED25519, retrievedKey.getAlgorithm());

                // Clean up
                key.close();
                retrievedKey.close();
            }
        }
    }

    @Test
    void testProfileOperations() throws AskarException {
        try (Store store = Store.provision(storeUri, "raw", null, null, false)) {
            // Create additional profiles
            String profile1 = store.createProfile("profile1");
            assertEquals("profile1", profile1);

            String profile2 = store.createProfile(null); // Auto-generated name
            assertNotNull(profile2);
            assertFalse(profile2.isEmpty());

            // List profiles
            List<String> profiles = store.listProfiles();
            assertTrue(profiles.contains("default")); // Default profile should exist
            assertTrue(profiles.contains("profile1"));
            assertTrue(profiles.contains(profile2));

            // Test operations with specific profile
            try (Session session = store.session("profile1").open()) {
                session.insert("category1", "name1", "value1".getBytes(), null, null);

                Entry entry = session.fetch("category1", "name1", false);
                assertNotNull(entry);
                assertEquals("value1", entry.getValueString());
            }

            // Verify entry is not visible from default profile
            try (Session session = store.session().open()) {
                Entry entry = session.fetch("category1", "name1", false);
                assertNull(entry);
            }

            // Remove profile
            boolean removed = store.removeProfile(profile2);
            assertTrue(removed);

            profiles = store.listProfiles();
            assertFalse(profiles.contains(profile2));
        }
    }

    @Test
    void testGenerateRawKey() throws AskarException {
        String rawKey = Store.generateRawKey(null);
        assertNotNull(rawKey);
        assertFalse(rawKey.isEmpty());

        // Test with seed
        byte[] seed = new byte[32];
        for (int i = 0; i < seed.length; i++) {
            seed[i] = (byte) i;
        }

        String rawKeyFromSeed = Store.generateRawKey(seed);
        assertNotNull(rawKeyFromSeed);
        assertFalse(rawKeyFromSeed.isEmpty());

        // Should be deterministic with same seed
        String rawKeyFromSeed2 = Store.generateRawKey(seed);
        assertEquals(rawKeyFromSeed, rawKeyFromSeed2);
    }
}