package org.hyperledger.aries.askar.examples;

import org.hyperledger.aries.askar.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Debug test for tags formatting issues.
 */
public class TagsDebugTest {

    public static void main(String[] args) {
        try {
            System.out.println("=== Tags Debug Test ===");
            
            String storeUri = "sqlite://tags-debug-test.db";
            String rawKey = "3Mn5Pg6YkqYhzKFjpzE5LBV7LU7vGK8d5V7v2Q8N9k7K";
            
            try (StoreJNI store = StoreJNI.provision(storeUri, "raw", rawKey, null, true)) {
                System.out.println("✅ Store provisioned successfully");
                
                try (StoreJNI.SessionJNI session = store.session().open()) {
                    System.out.println("✅ Session created successfully");
                    
                    // Test 1: Simple string tags (should work)
                    System.out.println("\n1. Testing simple string tags...");
                    Map<String, Object> simpleTags = new HashMap<>();
                    simpleTags.put("type", "test");
                    
                    try {
                        session.insert("test_category", "test_key_simple", "test_value".getBytes(), simpleTags, null);
                        System.out.println("✅ Simple tags succeeded");
                    } catch (Exception e) {
                        System.out.println("❌ Simple tags failed: " + e.getMessage());
                    }
                    
                    // Test 2: Mixed type tags (might fail)
                    System.out.println("\n2. Testing mixed type tags...");
                    Map<String, Object> mixedTags = new HashMap<>();
                    mixedTags.put("type", "test");
                    mixedTags.put("version", 1);  // Integer
                    mixedTags.put("priority", "high");
                    
                    try {
                        session.insert("test_category", "test_key_mixed", "test_value".getBytes(), mixedTags, null);
                        System.out.println("✅ Mixed tags succeeded");
                    } catch (Exception e) {
                        System.out.println("❌ Mixed tags failed: " + e.getMessage());
                    }
                    
                    // Test 3: String-only tags
                    System.out.println("\n3. Testing string-only tags...");
                    Map<String, Object> stringTags = new HashMap<>();
                    stringTags.put("type", "test");
                    stringTags.put("version", "1");  // String instead of integer
                    stringTags.put("priority", "high");
                    
                    try {
                        session.insert("test_category", "test_key_string", "test_value".getBytes(), stringTags, null);
                        System.out.println("✅ String-only tags succeeded");
                    } catch (Exception e) {
                        System.out.println("❌ String-only tags failed: " + e.getMessage());
                    }
                    
                    // Test 4: Empty tags
                    System.out.println("\n4. Testing empty tags...");
                    Map<String, Object> emptyTags = new HashMap<>();
                    
                    try {
                        session.insert("test_category", "test_key_empty", "test_value".getBytes(), emptyTags, null);
                        System.out.println("✅ Empty tags succeeded");
                    } catch (Exception e) {
                        System.out.println("❌ Empty tags failed: " + e.getMessage());
                    }
                    
                    // Test 5: Null tags
                    System.out.println("\n5. Testing null tags...");
                    try {
                        session.insert("test_category", "test_key_null", "test_value".getBytes(), null, null);
                        System.out.println("✅ Null tags succeeded");
                    } catch (Exception e) {
                        System.out.println("❌ Null tags failed: " + e.getMessage());
                    }
                    
                    // Check how many entries were actually inserted
                    System.out.println("\n6. Checking final count...");
                    int count = session.count("test_category", null);
                    System.out.println("✅ Total entries inserted: " + count);
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}