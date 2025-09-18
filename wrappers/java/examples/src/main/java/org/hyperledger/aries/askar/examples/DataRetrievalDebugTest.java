package org.hyperledger.aries.askar.examples;

import org.hyperledger.aries.askar.AskarNative;
import org.hyperledger.aries.askar.EntryOperation;

/**
 * Dedicated test to diagnose Data Retrieval issues
 */
public class DataRetrievalDebugTest {

    public static void main(String[] args) {
        System.out.println("=== Data Retrieval Debug Test ===");
        
        try {
            // 1. Setup store
            String rawKey = AskarNative.storeGenerateRawKey(null);
            long storeHandle = AskarNative.storeProvision("sqlite://:memory:", "raw", rawKey, null, true);
            System.out.println("✅ Store provisioned: " + storeHandle);
            
            // 2. Test different session approaches
            testSingleSessionApproach(storeHandle);
            testNonTransactionApproach(storeHandle);
            testFileBasedApproach();
            
            AskarNative.storeClose(storeHandle);
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testSingleSessionApproach(long storeHandle) {
        System.out.println("\n=== Test 1: Single Session (No Transaction) ===");
        
        try {
            // Use single session for insert and retrieve
            long session = AskarNative.sessionStart(storeHandle, null, false);
            System.out.println("Started single session: " + session);
            
            // Insert
            AskarNative.sessionUpdate(session, EntryOperation.INSERT.getValue(),
                "debug", "test1", "single session data".getBytes(), null, 0);
            System.out.println("✅ Data inserted in single session");
            
            // Immediate count
            int count1 = AskarNative.sessionCount(session, "debug", null);
            System.out.println("Count immediately after insert: " + count1);
            
            // Try fetch
            if (count1 > 0) {
                long entry = AskarNative.sessionFetch(session, "debug", "test1", false);
                if (entry != 0) {
                    String value = new String(AskarNative.entryListGetValue(entry, 0));
                    System.out.println("✅ Retrieved: " + value);
                    AskarNative.entryListFree(entry);
                } else {
                    System.out.println("❌ Fetch failed despite count > 0");
                }
            }
            
            AskarNative.sessionClose(session, true);
            System.out.println("Session closed with commit");
            
        } catch (Exception e) {
            System.out.println("❌ Single session test failed: " + e.getMessage());
        }
    }
    
    private static void testNonTransactionApproach(long storeHandle) {
        System.out.println("\n=== Test 2: Non-Transaction Session ===");
        
        try {
            // Use non-transaction session
            long session = AskarNative.sessionStart(storeHandle, null, false);
            System.out.println("Started non-transaction session: " + session);
            
            // Insert
            AskarNative.sessionUpdate(session, EntryOperation.INSERT.getValue(),
                "debug", "test2", "non-transaction data".getBytes(), 
                "{\"type\":\"test\"}", 0);
            System.out.println("✅ Data inserted in non-transaction session");
            
            // Immediate retrieval
            int count = AskarNative.sessionCount(session, "debug", null);
            System.out.println("Count in same session: " + count);
            
            // Close and reopen
            AskarNative.sessionClose(session, false);
            
            // New session for reading
            long readSession = AskarNative.sessionStart(storeHandle, null, false);
            int newCount = AskarNative.sessionCount(readSession, "debug", null);
            System.out.println("Count in new session: " + newCount);
            
            AskarNative.sessionClose(readSession, false);
            
        } catch (Exception e) {
            System.out.println("❌ Non-transaction test failed: " + e.getMessage());
        }
    }
    
    private static void testFileBasedApproach() {
        System.out.println("\n=== Test 3: File-based Store ===");
        
        try {
            // Try with actual file instead of memory
            String dbFile = "debug_test.db";
            String uri = "sqlite://" + dbFile;
            
            String rawKey = AskarNative.storeGenerateRawKey(null);
            long store = AskarNative.storeProvision(uri, "raw", rawKey, null, true);
            System.out.println("✅ File-based store provisioned: " + store);
            
            long session = AskarNative.sessionStart(store, null, false);
            
            // Insert
            AskarNative.sessionUpdate(session, EntryOperation.INSERT.getValue(),
                "file", "test", "file-based data".getBytes(), null, 0);
            System.out.println("✅ Data inserted to file");
            
            // Count
            int count = AskarNative.sessionCount(session, "file", null);
            System.out.println("File-based count: " + count);
            
            AskarNative.sessionClose(session, true);
            AskarNative.storeClose(store);
            
            // Try to delete file
            try {
                java.io.File file = new java.io.File(dbFile);
                if (file.exists()) {
                    file.delete();
                }
            } catch (Exception ignored) {}
            
        } catch (Exception e) {
            System.out.println("❌ File-based test failed: " + e.getMessage());
        }
    }
}