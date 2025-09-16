import org.hyperledger.aries.askar.*;

public class minimal_test {
    public static void main(String[] args) {
        try {
            System.out.println("=== Minimal FFI Compatibility Test ===");
            
            // Test 1: Basic library initialization
            System.out.println("1. Testing library initialization...");
            LibraryLoader.setMaxLogLevel(1); // ERROR level only
            System.out.println("✅ Library initialized successfully");
            
            // Test 2: Version check
            System.out.println("2. Testing version call...");
            String version = LibraryLoader.getVersion();
            System.out.println("✅ Version: " + version);
            
            // Test 3: Store provisioning (where crash occurred before)
            System.out.println("3. Testing store provisioning...");
            String storeUri = "sqlite://minimal_test.db";
            String rawKey = "3Mn5Pg6YkqYhzKFjpzE5LBV7LU7vGK8d5V7v2Q8N9k7K";
            
            try (Store store = Store.provision(storeUri, "raw", rawKey, null, true)) {
                System.out.println("✅ Store provisioned successfully");
                
                // Test 4: Session creation (minimal)
                System.out.println("4. Testing session creation...");
                try (Session session = store.session().open()) {
                    System.out.println("✅ Session created successfully");
                    
                    // Test 5: Simple count operation (should be safe)
                    System.out.println("5. Testing count operation...");
                    int count = session.count("test_category", null);
                    System.out.println("✅ Count result: " + count);
                    
                    System.out.println("6. Testing basic insert...");
                    session.insert("test_category", "test_key", "test_value".getBytes(), null, null);
                    System.out.println("✅ Insert successful");
                    
                    System.out.println("7. Testing count after insert...");
                    count = session.count("test_category", null);
                    System.out.println("✅ Count after insert: " + count);
                    
                    // This is where the crash might occur
                    System.out.println("8. Testing fetch operation...");
                    Entry entry = session.fetch("test_category", "test_key", false);
                    if (entry != null) {
                        System.out.println("✅ Fetch successful: " + entry.getValueString());
                    } else {
                        System.out.println("❌ Fetch returned null");
                    }
                    
                    System.out.println("9. Testing fetchAll operation (potential crash point)...");
                    java.util.List<Entry> entries = session.fetchAll("test_category", null, null, null, false, false);
                    System.out.println("✅ FetchAll successful, found " + entries.size() + " entries");
                    
                } catch (Exception e) {
                    System.out.println("❌ Session operation failed: " + e.getMessage());
                    e.printStackTrace();
                }
            } catch (Exception e) {
                System.out.println("❌ Store operation failed: " + e.getMessage());
                e.printStackTrace();
            }
            
            System.out.println("\n🎉 All tests completed successfully!");
            
        } catch (Exception e) {
            System.out.println("❌ Minimal test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}