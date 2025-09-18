package org.hyperledger.aries.askar.examples;

import org.hyperledger.aries.askar.AskarNative;
import org.hyperledger.aries.askar.EntryOperation;

/**
 * Test to verify parameter passing and data consistency
 */
public class ParameterDebugTest {

    public static void main(String[] args) {
        System.out.println("=== Parameter Debug Test ===");
        
        try {
            String rawKey = AskarNative.storeGenerateRawKey(null);
            long storeHandle = AskarNative.storeProvision("sqlite://:memory:", "raw", rawKey, null, true);
            long session = AskarNative.sessionStart(storeHandle, null, false);
            
            System.out.println("Store: " + storeHandle + ", Session: " + session);
            
            // Test 1: Simplest possible data
            testSimplestData(session);
            
            // Test 2: Different categories
            testDifferentCategories(session);
            
            // Test 3: Verify session state
            testSessionState(session);
            
            AskarNative.sessionClose(session, false);
            AskarNative.storeClose(storeHandle);
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testSimplestData(long session) {
        System.out.println("\n=== Test 1: Simplest Data ===");
        
        try {
            // Single character category and name, single byte value
            AskarNative.sessionUpdate(session, EntryOperation.INSERT.getValue(),
                "A", "B", new byte[]{1}, null, 0);
            System.out.println("✅ Inserted A/B/[1]");
            
            int count = AskarNative.sessionCount(session, "A", null);
            System.out.println("Count for category 'A': " + count);
            
            // Try different category
            count = AskarNative.sessionCount(session, null, null);
            System.out.println("Count for all categories (null): " + count);
            
        } catch (Exception e) {
            System.out.println("❌ Simplest data test failed: " + e.getMessage());
        }
    }
    
    private static void testDifferentCategories(long session) {
        System.out.println("\n=== Test 2: Different Categories ===");
        
        String[] categories = {"cat1", "category2", "c"};
        
        for (String cat : categories) {
            try {
                AskarNative.sessionUpdate(session, EntryOperation.INSERT.getValue(),
                    cat, "item", ("data for " + cat).getBytes(), null, 0);
                System.out.println("✅ Inserted to category: " + cat);
                
                int count = AskarNative.sessionCount(session, cat, null);
                System.out.println("Count for '" + cat + "': " + count);
                
            } catch (Exception e) {
                System.out.println("❌ Failed for category '" + cat + "': " + e.getMessage());
            }
        }
    }
    
    private static void testSessionState(long session) {
        System.out.println("\n=== Test 3: Session State ===");
        
        try {
            // Try to count with empty category
            int count1 = AskarNative.sessionCount(session, "", null);
            System.out.println("Count for empty category: " + count1);
            
            // Count all with null category  
            int count2 = AskarNative.sessionCount(session, null, null);
            System.out.println("Count for null category: " + count2);
            
            // Check if session is still valid by trying a simple operation
            System.out.println("Session appears valid: " + (session != 0));
            
        } catch (Exception e) {
            System.out.println("❌ Session state test failed: " + e.getMessage());
        }
    }
}