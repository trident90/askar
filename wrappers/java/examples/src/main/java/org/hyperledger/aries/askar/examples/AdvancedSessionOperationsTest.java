package org.hyperledger.aries.askar.examples;

import org.hyperledger.aries.askar.*;
import java.io.File;

/**
 * Comprehensive test for advanced session operations.
 */
public class AdvancedSessionOperationsTest {

    public static void main(String[] args) {
        try {
            System.out.println("=== Advanced Session Operations Test ===");
            
            // Test database file
            String dbPath = "test_advanced_session.db";
            String uri = "sqlite://" + dbPath;
            
            // Clean up any existing test file
            cleanupFile(dbPath);
            
            // Test 1: Create and provision a store
            System.out.println("\n1. Setting up test store and session...");
            long storeHandle = AskarNative.storeProvision(uri, "raw", "test_password", "default", true);
            if (storeHandle == 0) {
                System.out.println("❌ Failed to provision store");
                return;
            }
            System.out.println("✅ Store provisioned: " + storeHandle);
            
            // Start a session
            long sessionHandle = AskarNative.sessionStart(storeHandle, "default", false);
            if (sessionHandle == 0) {
                System.out.println("❌ Failed to start session");
                AskarNative.storeClose(storeHandle);
                return;
            }
            System.out.println("✅ Session started: " + sessionHandle);
            
            // Test 2: Generate keys for testing
            System.out.println("\n2. Generating test keys...");
            long testKey1 = AskarNative.keyGenerate("ed25519", null, false);
            long testKey2 = AskarNative.keyGenerate("x25519", null, false);
            
            if (testKey1 == 0 || testKey2 == 0) {
                System.out.println("❌ Failed to generate test keys");
                AskarNative.sessionClose(sessionHandle, false);
                AskarNative.storeClose(storeHandle);
                return;
            }
            System.out.println("✅ Test keys generated - Ed25519: " + testKey1 + ", X25519: " + testKey2);
            
            // Test 3: Insert keys into session
            System.out.println("\n3. Testing sessionInsertKey...");
            try {
                AskarNative.sessionInsertKey(sessionHandle, testKey1, "test_signing_key", 
                                           "metadata for signing key", "tag1=value1,tag2=value2", 0);
                System.out.println("✅ Inserted signing key successfully");
                
                AskarNative.sessionInsertKey(sessionHandle, testKey2, "test_encryption_key", 
                                           "metadata for encryption key", "tag1=value1,tag3=value3", 0);
                System.out.println("✅ Inserted encryption key successfully");
            } catch (Exception e) {
                System.out.println("❌ sessionInsertKey failed: " + e.getMessage());
            }
            
            // Test 4: Fetch specific key
            System.out.println("\n4. Testing sessionFetchKey...");
            try {
                long keyEntryList = AskarNative.sessionFetchKey(sessionHandle, "test_signing_key", false);
                if (keyEntryList != 0) {
                    System.out.println("✅ Fetched signing key successfully, entry list: " + keyEntryList);
                    // Note: In a real implementation, you'd have methods to extract key from KeyEntryList
                    // For now, we just verify the operation succeeded
                } else {
                    System.out.println("❌ Failed to fetch signing key");
                }
            } catch (Exception e) {
                System.out.println("❌ sessionFetchKey failed: " + e.getMessage());
            }
            
            // Test 5: Fetch all keys
            System.out.println("\n5. Testing sessionFetchAllKeys...");
            try {
                long allKeysEntryList = AskarNative.sessionFetchAllKeys(sessionHandle, null, 10, false);
                if (allKeysEntryList != 0) {
                    System.out.println("✅ Fetched all keys successfully, entry list: " + allKeysEntryList);
                } else {
                    System.out.println("❌ Failed to fetch all keys");
                }
            } catch (Exception e) {
                System.out.println("❌ sessionFetchAllKeys failed: " + e.getMessage());
            }
            
            // Test 6: Update key metadata
            System.out.println("\n6. Testing sessionUpdateKey...");
            try {
                AskarNative.sessionUpdateKey(sessionHandle, "test_signing_key", 
                                           "updated metadata for signing key", "tag1=updated,tag2=value2", 0);
                System.out.println("✅ Updated signing key metadata successfully");
            } catch (Exception e) {
                System.out.println("❌ sessionUpdateKey failed: " + e.getMessage());
            }
            
            // Test 7: Remove specific key
            System.out.println("\n7. Testing sessionRemoveKey...");
            try {
                AskarNative.sessionRemoveKey(sessionHandle, "test_encryption_key");
                System.out.println("✅ Removed encryption key successfully");
                
                // Verify removal by trying to fetch
                long removedKeyList = AskarNative.sessionFetchKey(sessionHandle, "test_encryption_key", false);
                if (removedKeyList == 0) {
                    System.out.println("✅ Verified key removal - key no longer exists");
                } else {
                    System.out.println("⚠️  Key still exists after removal");
                }
            } catch (Exception e) {
                System.out.println("❌ sessionRemoveKey failed: " + e.getMessage());
            }
            
            // Commit session changes
            AskarNative.sessionClose(sessionHandle, true);
            System.out.println("✅ Session committed and closed");
            
            // Test 8: Scan operations
            System.out.println("\n8. Testing scan operations...");
            try {
                long scanHandle = AskarNative.scanStart(storeHandle, "default", null, null, 0, 10, null, false);
                if (scanHandle != 0) {
                    System.out.println("✅ Scan started successfully, handle: " + scanHandle);
                    
                    // Get scan results
                    long scanResults = AskarNative.scanNext(scanHandle);
                    if (scanResults != 0) {
                        System.out.println("✅ Scan next succeeded, results: " + scanResults);
                    } else {
                        System.out.println("⚠️  No scan results (store may be empty)");
                    }
                    
                    // Free scan resources
                    AskarNative.scanFree(scanHandle);
                    System.out.println("✅ Scan resources freed");
                } else {
                    System.out.println("❌ Failed to start scan");
                }
            } catch (Exception e) {
                System.out.println("❌ Scan operations failed: " + e.getMessage());
            }
            
            // Start new session for removal testing
            System.out.println("\n9. Testing sessionRemoveAll...");
            long newSessionHandle = AskarNative.sessionStart(storeHandle, "default", false);
            if (newSessionHandle != 0) {
                try {
                    long removedCount = AskarNative.sessionRemoveAll(newSessionHandle, null, null);
                    System.out.println("✅ sessionRemoveAll completed, removed count: " + removedCount);
                } catch (Exception e) {
                    System.out.println("❌ sessionRemoveAll failed: " + e.getMessage());
                }
                
                AskarNative.sessionClose(newSessionHandle, true);
                System.out.println("✅ Cleanup session closed");
            }
            
            // Test 10: Insert regular entries (non-key data)
            System.out.println("\n10. Testing regular entry operations...");
            long regularSessionHandle = AskarNative.sessionStart(storeHandle, "default", false);
            if (regularSessionHandle != 0) {
                try {
                    // Insert regular entry
                    AskarNative.sessionUpdate(regularSessionHandle, (byte)1, "test_category", "test_entry", 
                                            "test data".getBytes(), "entry_tag=test", 0);
                    System.out.println("✅ Inserted regular entry successfully");
                    
                    // Fetch regular entry
                    long entryList = AskarNative.sessionFetch(regularSessionHandle, "test_category", "test_entry", false);
                    if (entryList != 0) {
                        System.out.println("✅ Fetched regular entry successfully: " + entryList);
                    } else {
                        System.out.println("❌ Failed to fetch regular entry");
                    }
                    
                    // Fetch all regular entries
                    long allEntries = AskarNative.sessionFetchAll(regularSessionHandle, "test_category", null, 10, null, false, false);
                    if (allEntries != 0) {
                        System.out.println("✅ Fetched all regular entries successfully: " + allEntries);
                    } else {
                        System.out.println("❌ Failed to fetch all regular entries");
                    }
                    
                } catch (Exception e) {
                    System.out.println("❌ Regular entry operations failed: " + e.getMessage());
                }
                
                AskarNative.sessionClose(regularSessionHandle, true);
                System.out.println("✅ Regular session closed");
            }
            
            // Clean up
            AskarNative.keyFree(testKey1);
            AskarNative.keyFree(testKey2);
            AskarNative.storeClose(storeHandle);
            cleanupFile(dbPath);
            
            System.out.println("\n🎉 Advanced Session Operations Test completed!");
            
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void cleanupFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
            System.out.println("Cleaned up file: " + filePath);
        }
    }
}