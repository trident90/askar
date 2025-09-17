package org.hyperledger.aries.askar.examples;

import org.hyperledger.aries.askar.AskarNative;
import java.io.File;

/**
 * Basic example demonstrating store operations using Native API.
 * Shows complete store lifecycle with data operations.
 */
public class BasicStoreExample {

    public static void main(String[] args) {
        try {
            System.out.println("=== Askar Native API Basic Store Example ===");
            
            // Test database file
            String dbPath = "test_basic_store.db";
            String storeUri = "sqlite://" + dbPath;
            String rawKey = "basic_store_example_key_123456789012345678901234567890";

            // Clean up any existing test file
            cleanupFile(dbPath);

            System.out.println("1. Testing version...");
            String version = AskarNative.getVersion();
            System.out.println("✅ Askar version: " + version);

            // Provision a new store
            System.out.println("\n2. Provisioning store...");
            long storeHandle = AskarNative.storeProvision(storeUri, "raw", rawKey, "default", true);
            
            if (storeHandle != 0) {
                System.out.println("✅ Store provisioned successfully: " + storeHandle);

                // Demonstrate basic operations
                demonstrateBasicOperations(storeHandle);
                
                // Demonstrate profile operations
                demonstrateProfileOperations(storeHandle);

                // Close store
                AskarNative.storeClose(storeHandle);
                System.out.println("✅ Store closed successfully");
            } else {
                System.out.println("⚠️  Store provisioning failed");
                System.out.println("This may indicate native library setup issues");
            }

            // Clean up
            cleanupFile(dbPath);
            System.out.println("\n🎉 Basic Store Example completed successfully!");

        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void demonstrateBasicOperations(long storeHandle) {
        System.out.println("\n=== Basic Data Operations ===");

        try {
            // Start a session
            long sessionHandle = AskarNative.sessionStart(storeHandle, "default", false);
            if (sessionHandle == 0) {
                System.out.println("❌ Failed to start session");
                return;
            }
            System.out.println("✅ Session started: " + sessionHandle);
            
            // Insert some test data
            System.out.println("\n3. Testing data insertion...");
            AskarNative.sessionUpdate(
                sessionHandle,
                (byte)1, // Insert operation
                "credentials",
                "user_credential_1",
                createSampleCredential().getBytes(),
                "{\"type\": \"credential\", \"issuer\": \"example_org\", \"status\": \"active\"}",
                0
            );
            System.out.println("✅ Inserted credential 1");

            AskarNative.sessionUpdate(
                sessionHandle,
                (byte)1, // Insert operation
                "credentials", 
                "user_credential_2",
                "Sample credential data 2".getBytes(),
                "{\"type\": \"credential\", \"issuer\": \"example_org\", \"status\": \"revoked\"}",
                0
            );
            System.out.println("✅ Inserted credential 2");

            // Count entries
            System.out.println("\n4. Testing entry count...");
            int totalCount = AskarNative.sessionCount(sessionHandle, "credentials", null);
            System.out.println("✅ Total credentials: " + totalCount);

            // Count with tag filter
            int activeCount = AskarNative.sessionCount(sessionHandle, "credentials", "{\"status\": \"active\"}");
            System.out.println("✅ Active credentials: " + activeCount);

            // Fetch specific entry
            System.out.println("\n5. Testing entry fetch...");
            long entryList = AskarNative.sessionFetch(sessionHandle, "credentials", "user_credential_1", false);
            if (entryList != 0) {
                String category = AskarNative.entryListGetCategory(entryList, 0);
                String name = AskarNative.entryListGetName(entryList, 0);
                byte[] value = AskarNative.entryListGetValue(entryList, 0);
                String tags = AskarNative.entryListGetTags(entryList, 0);
                
                System.out.println("✅ Retrieved entry:");
                System.out.println("   Category: " + category);
                System.out.println("   Name: " + name);
                System.out.println("   Value: " + new String(value));
                System.out.println("   Tags: " + tags);
                
                AskarNative.entryListFree(entryList);
            } else {
                System.out.println("⚠️  Entry not found or fetch failed");
            }

            // Fetch all entries
            System.out.println("\n6. Testing fetch all entries...");
            long allEntries = AskarNative.sessionFetchAll(sessionHandle, "credentials", null, 10, null, false, false);
            if (allEntries != 0) {
                int count = AskarNative.entryListCount(allEntries);
                System.out.println("✅ Fetched " + count + " entries:");
                
                for (int i = 0; i < count; i++) {
                    String name = AskarNative.entryListGetName(allEntries, i);
                    String tags = AskarNative.entryListGetTags(allEntries, i);
                    System.out.println("   " + (i + 1) + ". " + name + " - " + tags);
                }
                
                AskarNative.entryListFree(allEntries);
            } else {
                System.out.println("⚠️  No entries found or fetch failed");
            }

            // Close session
            AskarNative.sessionClose(sessionHandle, true);
            System.out.println("✅ Session closed and committed");

        } catch (Exception e) {
            System.out.println("⚠️  Data operations failed: " + e.getMessage());
            System.out.println("This may indicate native library setup issues");
        }
    }
    
    private static void demonstrateProfileOperations(long storeHandle) {
        System.out.println("\n=== Profile Operations ===");
        
        try {
            // Get current profile name
            String currentProfile = AskarNative.storeGetProfileName(storeHandle);
            System.out.println("✅ Current profile: " + currentProfile);
            
            // Get default profile
            String defaultProfile = AskarNative.storeGetDefaultProfile(storeHandle);
            System.out.println("✅ Default profile: " + defaultProfile);
            
            // Create a new profile
            String newProfile = AskarNative.storeCreateProfile(storeHandle, "test_profile");
            System.out.println("✅ Created profile: " + newProfile);
            
            // List all profiles
            String[] profiles = AskarNative.storeListProfiles(storeHandle);
            System.out.println("✅ Available profiles:");
            for (String profile : profiles) {
                System.out.println("   - " + profile);
            }
            
            // Remove the test profile
            boolean removed = AskarNative.storeRemoveProfile(storeHandle, "test_profile");
            System.out.println("✅ Test profile removed: " + removed);
            
        } catch (Exception e) {
            System.out.println("⚠️  Profile operations failed: " + e.getMessage());
        }
    }
    
    private static String createSampleCredential() {
        return "{\n" +
               "  \"@context\": \"https://www.w3.org/2018/credentials/v1\",\n" +
               "  \"type\": [\"VerifiableCredential\", \"UniversityDegreeCredential\"],\n" +
               "  \"issuer\": \"https://example.edu/issuers/14\",\n" +
               "  \"credentialSubject\": {\n" +
               "    \"id\": \"did:example:ebfeb1f712ebc6f1c276e12ec21\",\n" +
               "    \"degree\": {\n" +
               "      \"type\": \"BachelorDegree\",\n" +
               "      \"name\": \"Bachelor of Science in Computer Science\"\n" +
               "    }\n" +
               "  }\n" +
               "}";
    }
    
    private static void cleanupFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
            System.out.println("Cleaned up file: " + filePath);
        }
    }
}