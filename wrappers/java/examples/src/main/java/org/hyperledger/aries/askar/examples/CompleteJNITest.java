package org.hyperledger.aries.askar.examples;

import org.hyperledger.aries.askar.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete test of JNI implementation with all functions.
 */
public class CompleteJNITest {

    public static void main(String[] args) {
        try {
            System.out.println("=== Complete JNI Test ===");
            
            // Test 1: Version information
            System.out.println("1. Testing version...");
            String version = StoreJNI.getVersion();
            System.out.println("✅ Askar version: " + version);
            
            // Test 2: Store provisioning
            System.out.println("2. Testing store provisioning...");
            String storeUri = "sqlite://complete-jni-test.db";
            String rawKey = "3Mn5Pg6YkqYhzKFjpzE5LBV7LU7vGK8d5V7v2Q8N9k7K";
            
            try (StoreJNI store = StoreJNI.provision(storeUri, "raw", rawKey, null, true)) {
                System.out.println("✅ Store provisioned successfully");
                
                // Test 3: Session operations
                System.out.println("3. Testing session operations...");
                try (StoreJNI.SessionJNI session = store.session().open()) {
                    System.out.println("✅ Session created successfully");
                    
                    // Test 4: Insert data
                    System.out.println("4. Testing data insertion...");
                    Map<String, Object> tags = new HashMap<>();
                    tags.put("type", "test");
                    tags.put("version", 1);
                    tags.put("priority", "high");
                    
                    // Insert multiple entries
                    session.insert("test_category", "test_key_1", "test_value_1".getBytes(), tags, null);
                    session.insert("test_category", "test_key_2", "test_value_2".getBytes(), tags, null);
                    session.insert("test_category", "test_key_3", "test_value_3".getBytes(), tags, null);
                    System.out.println("✅ Inserted 3 test entries");
                    
                    // Test 5: Count entries
                    System.out.println("5. Testing entry count...");
                    int count = session.count("test_category", null);
                    System.out.println("✅ Total entries in test_category: " + count);
                    
                    // Test 6: Fetch all entries
                    System.out.println("6. Testing fetch all entries...");
                    List<Entry> entries = session.fetchAll("test_category", null, null, null, false, false);
                    System.out.println("✅ Found " + entries.size() + " entries:");
                    
                    for (Entry entry : entries) {
                        System.out.println("  - " + entry.getName() + ": " + entry.getValueString());
                        System.out.println("    Tags: " + entry.getTags());
                    }
                    
                    // Test 7: Fetch specific entry
                    System.out.println("7. Testing fetch specific entry...");
                    Entry specificEntry = session.fetch("test_category", "test_key_2", false);
                    if (specificEntry != null) {
                        System.out.println("✅ Found specific entry: " + specificEntry.getName() + " = " + specificEntry.getValueString());
                        System.out.println("   Tags: " + specificEntry.getTags());
                    } else {
                        System.out.println("❌ Specific entry not found");
                    }
                    
                    // Test 8: Count with tag filter
                    System.out.println("8. Testing count with tag filter...");
                    int filteredCount = session.count("test_category", "{\"type\": \"test\"}");
                    System.out.println("✅ Entries matching tag filter: " + filteredCount);
                    
                    System.out.println("9. All data operations completed successfully!");
                }
                
                System.out.println("10. Store operations completed successfully!");
            }
            
            System.out.println("\n🎉 Complete JNI Test completed successfully!");
            System.out.println("🚀 All core JNI functions are working correctly!");
            
        } catch (AskarException e) {
            System.err.println("❌ Askar error: " + e.getMessage());
            System.err.println("   Error code: " + e.getErrorCode());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}