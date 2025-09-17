package org.hyperledger.aries.askar.examples;

import org.hyperledger.aries.askar.AskarNative;
import java.io.File;

/**
 * Complete test of Native API implementation with all major functions.
 * This demonstrates the full range of Askar capabilities through direct native calls.
 */
public class CompleteJNITest {

    public static void main(String[] args) {
        try {
            System.out.println("=== Complete Native API Test ===");
            
            // Test database file
            String dbPath = "complete-native-test.db";
            String storeUri = "sqlite://" + dbPath;
            String rawKey = "complete_test_key_123456789012345678901234567890_abcdef";

            // Clean up any existing test file
            cleanupFile(dbPath);
            
            // Test 1: Version information
            System.out.println("1. Testing version information...");
            String version = AskarNative.getVersion();
            System.out.println("✅ Askar version: " + version);
            
            // Test 2: Key operations
            System.out.println("\n2. Testing key operations...");
            testKeyOperations();
            
            // Test 3: Store provisioning
            System.out.println("\n3. Testing store provisioning...");
            long storeHandle = AskarNative.storeProvision(storeUri, "raw", rawKey, "default", true);
            
            if (storeHandle != 0) {
                System.out.println("✅ Store provisioned successfully: " + storeHandle);
                
                // Test 4: Store management operations
                System.out.println("\n4. Testing store management...");
                testStoreManagement(storeHandle);
                
                // Test 5: Session and data operations
                System.out.println("\n5. Testing session operations...");
                testSessionOperations(storeHandle);
                
                // Test 6: Advanced key storage operations
                System.out.println("\n6. Testing advanced key storage...");
                testKeyStorageOperations(storeHandle);
                
                // Test 7: Scan operations
                System.out.println("\n7. Testing scan operations...");
                testScanOperations(storeHandle);
                
                // Close store
                AskarNative.storeClose(storeHandle);
                System.out.println("✅ Store closed successfully");
                
            } else {
                System.out.println("⚠️  Store provisioning failed - native library setup may be required");
            }
            
            // Clean up
            cleanupFile(dbPath);
            
            System.out.println("\n🎉 Complete Native API Test finished!");
            System.out.println("🚀 This demonstrates the full power of Askar Java wrapper!");
            
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testKeyOperations() {
        try {
            // Generate different types of keys
            long ed25519Key = AskarNative.keyGenerate("ed25519", null, false);
            long x25519Key = AskarNative.keyGenerate("x25519", null, false);
            
            if (ed25519Key != 0 && x25519Key != 0) {
                System.out.println("✅ Generated Ed25519 and X25519 keys");
                
                // Get key information
                String ed25519Alg = AskarNative.keyGetAlgorithm(ed25519Key);
                String x25519Alg = AskarNative.keyGetAlgorithm(x25519Key);
                byte[] ed25519Public = AskarNative.keyGetPublicBytes(ed25519Key);
                byte[] x25519Public = AskarNative.keyGetPublicBytes(x25519Key);
                
                System.out.println("   Ed25519 - Algorithm: " + ed25519Alg + ", Public key length: " + ed25519Public.length);
                System.out.println("   X25519 - Algorithm: " + x25519Alg + ", Public key length: " + x25519Public.length);
                
                // Test signing
                byte[] message = "Hello from Complete Test!".getBytes();
                byte[] signature = AskarNative.keySignMessage(ed25519Key, message, null);
                boolean isValid = AskarNative.keyVerifySignature(ed25519Key, message, signature, null);
                System.out.println("✅ Signature test: " + (isValid ? "PASSED" : "FAILED"));
                
                // Clean up keys
                AskarNative.keyFree(ed25519Key);
                AskarNative.keyFree(x25519Key);
                System.out.println("✅ Keys freed successfully");
            } else {
                System.out.println("⚠️  Key generation failed");
            }
        } catch (Exception e) {
            System.out.println("⚠️  Key operations failed: " + e.getMessage());
        }
    }
    
    private static void testStoreManagement(long storeHandle) {
        try {
            // Profile operations
            String currentProfile = AskarNative.storeGetProfileName(storeHandle);
            System.out.println("✅ Current profile: " + currentProfile);
            
            // Create and list profiles
            String testProfile = AskarNative.storeCreateProfile(storeHandle, "test_profile");
            System.out.println("✅ Created test profile: " + testProfile);
            
            String[] profiles = AskarNative.storeListProfiles(storeHandle);
            System.out.println("✅ Available profiles (" + profiles.length + "):");
            for (String profile : profiles) {
                System.out.println("   - " + profile);
            }
            
            // Remove test profile
            boolean removed = AskarNative.storeRemoveProfile(storeHandle, "test_profile");
            System.out.println("✅ Test profile removed: " + removed);
            
        } catch (Exception e) {
            System.out.println("⚠️  Store management failed: " + e.getMessage());
        }
    }
    
    private static void testSessionOperations(long storeHandle) {
        try {
            long sessionHandle = AskarNative.sessionStart(storeHandle, "default", false);
            if (sessionHandle == 0) {
                System.out.println("❌ Failed to start session");
                return;
            }
            
            // Insert test data with different categories
            AskarNative.sessionUpdate(
                sessionHandle, (byte)1, "documents", "contract_1", 
                createSampleDocument("Contract 1").getBytes(),
                "{\"type\": \"contract\", \"status\": \"active\", \"year\": \"2024\"}", 0
            );
            
            AskarNative.sessionUpdate(
                sessionHandle, (byte)1, "documents", "contract_2", 
                createSampleDocument("Contract 2").getBytes(),
                "{\"type\": \"contract\", \"status\": \"draft\", \"year\": \"2024\"}", 0
            );
            
            AskarNative.sessionUpdate(
                sessionHandle, (byte)1, "credentials", "degree_cert", 
                createSampleCredential().getBytes(),
                "{\"type\": \"education\", \"level\": \"bachelor\", \"verified\": true}", 0
            );
            
            System.out.println("✅ Inserted test data in multiple categories");
            
            // Count operations
            int totalDocs = AskarNative.sessionCount(sessionHandle, "documents", null);
            int activeDocs = AskarNative.sessionCount(sessionHandle, "documents", "{\"status\": \"active\"}");
            int totalCreds = AskarNative.sessionCount(sessionHandle, "credentials", null);
            
            System.out.println("✅ Document count - Total: " + totalDocs + ", Active: " + activeDocs);
            System.out.println("✅ Credential count: " + totalCreds);
            
            // Fetch operations
            long docEntries = AskarNative.sessionFetchAll(sessionHandle, "documents", null, 10, null, false, false);
            if (docEntries != 0) {
                int count = AskarNative.entryListCount(docEntries);
                System.out.println("✅ Fetched " + count + " document entries");
                AskarNative.entryListFree(docEntries);
            }
            
            // Specific entry fetch
            long specificEntry = AskarNative.sessionFetch(sessionHandle, "credentials", "degree_cert", false);
            if (specificEntry != 0) {
                String name = AskarNative.entryListGetName(specificEntry, 0);
                String tags = AskarNative.entryListGetTags(specificEntry, 0);
                System.out.println("✅ Fetched specific entry: " + name + " with tags: " + tags);
                AskarNative.entryListFree(specificEntry);
            }
            
            AskarNative.sessionClose(sessionHandle, true);
            System.out.println("✅ Session operations completed successfully");
            
        } catch (Exception e) {
            System.out.println("⚠️  Session operations failed: " + e.getMessage());
        }
    }
    
    private static void testKeyStorageOperations(long storeHandle) {
        try {
            long sessionHandle = AskarNative.sessionStart(storeHandle, "default", false);
            if (sessionHandle == 0) {
                System.out.println("❌ Failed to start session for key storage");
                return;
            }
            
            // Generate and store keys
            long signingKey = AskarNative.keyGenerate("ed25519", null, false);
            long encryptionKey = AskarNative.keyGenerate("x25519", null, false);
            
            if (signingKey != 0 && encryptionKey != 0) {
                // Insert keys into storage
                AskarNative.sessionInsertKey(
                    sessionHandle, signingKey, "main_signing_key", 
                    "{\"purpose\": \"document_signing\", \"created\": \"2024-01-01\"}", 
                    "{\"type\": \"signing\", \"curve\": \"ed25519\"}", 0
                );
                
                AskarNative.sessionInsertKey(
                    sessionHandle, encryptionKey, "main_encryption_key", 
                    "{\"purpose\": \"data_encryption\", \"created\": \"2024-01-01\"}", 
                    "{\"type\": \"encryption\", \"curve\": \"x25519\"}", 0
                );
                
                System.out.println("✅ Stored signing and encryption keys");
                
                // Fetch all stored keys
                long keyEntries = AskarNative.sessionFetchAllKeys(sessionHandle, null, 10, false);
                if (keyEntries != 0) {
                    int keyCount = AskarNative.keyEntryListCount(keyEntries);
                    System.out.println("✅ Found " + keyCount + " stored keys:");
                    
                    for (int i = 0; i < keyCount; i++) {
                        String keyName = AskarNative.keyEntryListGetName(keyEntries, i);
                        String keyAlgorithm = AskarNative.keyEntryListGetAlgorithm(keyEntries, i);
                        String keyMetadata = AskarNative.keyEntryListGetMetadata(keyEntries, i);
                        System.out.println("   " + (i + 1) + ". " + keyName + " (" + keyAlgorithm + ") - " + keyMetadata);
                    }
                    
                    AskarNative.keyEntryListFree(keyEntries);
                }
                
                AskarNative.keyFree(signingKey);
                AskarNative.keyFree(encryptionKey);
            }
            
            AskarNative.sessionClose(sessionHandle, true);
            System.out.println("✅ Key storage operations completed");
            
        } catch (Exception e) {
            System.out.println("⚠️  Key storage operations failed: " + e.getMessage());
        }
    }
    
    private static void testScanOperations(long storeHandle) {
        try {
            long scanHandle = AskarNative.scanStart(storeHandle, "default", "documents", null, 0, 10, null, false);
            if (scanHandle != 0) {
                System.out.println("✅ Scan started successfully");
                
                long scanResults = AskarNative.scanNext(scanHandle);
                if (scanResults != 0) {
                    int count = AskarNative.entryListCount(scanResults);
                    System.out.println("✅ Scan found " + count + " entries");
                } else {
                    System.out.println("✅ Scan completed (no more entries)");
                }
                
                AskarNative.scanFree(scanHandle);
                System.out.println("✅ Scan operations completed");
            }
        } catch (Exception e) {
            System.out.println("⚠️  Scan operations failed: " + e.getMessage());
        }
    }
    
    private static String createSampleDocument(String title) {
        return "{\n" +
               "  \"title\": \"" + title + "\",\n" +
               "  \"type\": \"legal_document\",\n" +
               "  \"content\": \"This is a sample legal document for testing purposes.\",\n" +
               "  \"created_date\": \"2024-01-01\",\n" +
               "  \"parties\": [\"Party A\", \"Party B\"]\n" +
               "}";
    }
    
    private static String createSampleCredential() {
        return "{\n" +
               "  \"@context\": \"https://www.w3.org/2018/credentials/v1\",\n" +
               "  \"type\": [\"VerifiableCredential\", \"UniversityDegreeCredential\"],\n" +
               "  \"issuer\": \"https://example.edu/issuers/14\",\n" +
               "  \"credentialSubject\": {\n" +
               "    \"id\": \"did:example:complete_test_subject\",\n" +
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