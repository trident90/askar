package org.hyperledger.aries.askar.examples;

import org.hyperledger.aries.askar.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Debug sessionUpdate INPUT error issue.
 */
public class DebugSessionUpdate {

    public static void main(String[] args) {
        try {
            System.out.println("=== Debugging sessionUpdate INPUT Error ===");
            
            String storeUri = "sqlite://debug_session_update.db";
            String rawKey = "3Mn5Pg6YkqYhzKFjpzE5LBV7LU7vGK8d5V7v2Q8N9k7K";
            
            try (StoreJNI store = StoreJNI.provision(storeUri, "raw", rawKey, null, true)) {
                System.out.println("✅ Store provisioned successfully");
                
                try (StoreJNI.SessionJNI session = store.session().open()) {
                    System.out.println("✅ Session created successfully");
                    
                    // Test simple insert with minimal data
                    System.out.println("🔍 Testing sessionUpdate with simple data...");
                    
                    Map<String, Object> tags = new HashMap<>();
                    tags.put("type", "test");
                    
                    String testValue = "simple_test_value";
                    byte[] valueBytes = testValue.getBytes("UTF-8");
                    
                    System.out.println("Parameters:");
                    System.out.println("  operation: " + EntryOperation.INSERT.getValue() + " (INSERT)");
                    System.out.println("  category: test_category");
                    System.out.println("  name: test_name");
                    System.out.println("  value length: " + valueBytes.length);
                    System.out.println("  value: " + testValue);
                    System.out.println("  tags: " + tags);
                    System.out.println("  expiry: null");
                    
                    // This should trigger the INPUT error
                    session.insert("test_category", "test_name", valueBytes, tags, null);
                    
                    System.out.println("✅ sessionUpdate succeeded (unexpected!)");
                    
                } catch (AskarException e) {
                    System.err.println("❌ AskarException: " + e.getMessage());
                    System.err.println("   Error code: " + e.getErrorCode());
                    e.printStackTrace();
                } catch (Exception e) {
                    System.err.println("❌ Other Exception: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Setup error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}