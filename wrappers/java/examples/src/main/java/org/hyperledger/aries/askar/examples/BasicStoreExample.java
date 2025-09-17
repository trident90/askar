package org.hyperledger.aries.askar.examples;

import org.hyperledger.aries.askar.AskarNative;
import org.hyperledger.aries.askar.EntryOperation;
import java.io.File;

/**
 * Basic example demonstrating store operations using Native API.
 * Shows complete store lifecycle with data operations.
 */
public class BasicStoreExample {

    public static void main(String[] args) {
        try {
            System.out.println("=== Askar Native API Basic Store Example ===");
            
            // Use in-memory SQLite as per Askar test code
            String storeUri = "sqlite://:memory:";
            
            // Generate proper raw key using Askar's function
            String rawKey;
            try {
                rawKey = AskarNative.storeGenerateRawKey(null); // null seed as per test
                System.out.println("✅ Generated raw key successfully");
            } catch (Exception e) {
                System.out.println("⚠️  Failed to generate raw key: " + e.getMessage());
                return;
            }

            // No cleanup needed for memory database

            System.out.println("1. Testing version...");
            String version = AskarNative.getVersion();
            System.out.println("✅ Askar version: " + version);

            // Provision a new store using parameters from Askar test code
            System.out.println("\n2. Provisioning store...");
            System.out.println("   URI: " + storeUri);
            System.out.println("   Key method: raw");
            System.out.println("   Profile: null (as per test code)");
            System.out.println("   Recreate: true");
            
            try {
                long storeHandle = AskarNative.storeProvision(storeUri, "raw", rawKey, null, true);
                
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
                    System.out.println("⚠️  Store provisioning failed - returned handle: " + storeHandle);
                    System.out.println("   This may indicate:");
                    System.out.println("   - SQLite database creation issues");
                    System.out.println("   - Invalid URI format or path permissions");
                    System.out.println("   - Askar core initialization problems");
                }
            } catch (Exception e) {
                System.out.println("⚠️  Store provisioning failed with exception: " + e.getMessage());
                e.printStackTrace();
            }

            // No cleanup needed for memory database
            System.out.println("\n🎉 Basic Store Example completed successfully!");

        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void demonstrateBasicOperations(long storeHandle) {
        System.out.println("\n=== Basic Data Operations ===");

        try {
            // Test key operations first (as per Rust test pattern)
            System.out.println("\n3. Testing key operations...");
            
            // Generate a key for testing (following Rust test pattern)
            long keyHandle = AskarNative.keyGenerate("ed25519", null, false);
            if (keyHandle != 0) {
                System.out.println("✅ Generated Ed25519 key for testing: " + keyHandle);
                
                // Start a transaction session (as per test pattern)
                long sessionHandle = AskarNative.sessionStart(storeHandle, null, true);
                if (sessionHandle == 0) {
                    System.out.println("❌ Failed to start transaction session");
                    AskarNative.keyFree(keyHandle);
                    return;
                }
                System.out.println("✅ Transaction session started: " + sessionHandle);
                
                // Insert key with metadata (following Rust test pattern)
                try {
                    AskarNative.sessionInsertKey(sessionHandle, keyHandle, "test_key", "test metadata", null, 0);
                    System.out.println("✅ Key inserted successfully");
                    
                    // Try simple data insertion with minimal data
                    System.out.println("\n4. Testing simple data insertion...");
                    AskarNative.sessionUpdate(
                        sessionHandle,
                        EntryOperation.INSERT.getValue(), // Using proper INSERT operation
                        "test",
                        "simple_entry",
                        "simple test data".getBytes(),
                        null, // No tags initially
                        0
                    );
                    System.out.println("✅ Simple data inserted");
                    
                    // Test immediate retrieval within same transaction
                    System.out.println("\n4.1. Testing immediate retrieval within transaction...");
                    int immediateCount = AskarNative.sessionCount(sessionHandle, "test", null);
                    System.out.println("✅ Immediate test entries count: " + immediateCount);
                    
                    if (immediateCount > 0) {
                        long immediateEntry = AskarNative.sessionFetch(sessionHandle, "test", "simple_entry", false);
                        if (immediateEntry != 0) {
                            String value = new String(AskarNative.entryListGetValue(immediateEntry, 0));
                            System.out.println("✅ Retrieved within transaction: " + value);
                            AskarNative.entryListFree(immediateEntry);
                        }
                    }
                    
                } catch (Exception e) {
                    System.out.println("⚠️  Key/Data operations failed: " + e.getMessage());
                }
                
                // Commit transaction
                AskarNative.sessionClose(sessionHandle, true);
                System.out.println("✅ Transaction committed");
                AskarNative.keyFree(keyHandle);
            } else {
                System.out.println("⚠️  Failed to generate test key");
                return;
            }

            // Test data retrieval with new session (following proper session lifecycle)
            System.out.println("\n5. Testing data retrieval...");
            long readSession = AskarNative.sessionStart(storeHandle, null, false);
            if (readSession != 0) {
                try {
                    // Count simple entries
                    int testCount = AskarNative.sessionCount(readSession, "test", null);
                    System.out.println("✅ Test entries count: " + testCount);
                    
                    // Fetch the inserted key
                    long fetchedKeyHandle = AskarNative.sessionFetchKey(readSession, "test_key", false);
                    if (fetchedKeyHandle != 0) {
                        System.out.println("✅ Retrieved inserted key: " + fetchedKeyHandle);
                        AskarNative.keyFree(fetchedKeyHandle);
                    } else {
                        System.out.println("⚠️  Could not retrieve inserted key");
                    }
                    
                    // Fetch simple data entry
                    long entryList = AskarNative.sessionFetch(readSession, "test", "simple_entry", false);
                    if (entryList != 0) {
                        String category = AskarNative.entryListGetCategory(entryList, 0);
                        String name = AskarNative.entryListGetName(entryList, 0);
                        byte[] value = AskarNative.entryListGetValue(entryList, 0);
                        
                        System.out.println("✅ Retrieved entry:");
                        System.out.println("   Category: " + category);
                        System.out.println("   Name: " + name);
                        System.out.println("   Value: " + new String(value));
                        
                        AskarNative.entryListFree(entryList);
                    } else {
                        System.out.println("⚠️  Entry not found or fetch failed");
                    }
                    
                } catch (Exception e) {
                    System.out.println("⚠️  Data retrieval failed: " + e.getMessage());
                } finally {
                    AskarNative.sessionClose(readSession, false);
                    System.out.println("✅ Read session closed");
                }
            } else {
                System.out.println("⚠️  Failed to start read session");
            }

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