package org.hyperledger.aries.askar.examples;

import org.hyperledger.aries.askar.*;
import java.io.File;
import java.util.Arrays;

/**
 * Comprehensive test for store management operations.
 */
public class StoreManagementTest {

    public static void main(String[] args) {
        try {
            System.out.println("=== Store Management Test ===");
            
            // Test database files
            String originalDbPath = "test_store_original.db";
            String copyDbPath = "test_store_copy.db";
            String originalUri = "sqlite://" + originalDbPath;
            String copyUri = "sqlite://" + copyDbPath;
            
            // Clean up any existing test files
            cleanupFiles(originalDbPath, copyDbPath);
            
            // Test 1: Create and provision a store
            System.out.println("\n1. Testing store provisioning...");
            long storeHandle = AskarNative.storeProvision(originalUri, "raw", "test_password", "default", true);
            if (storeHandle != 0) {
                System.out.println("✅ Store provisioned successfully, handle: " + storeHandle);
            } else {
                System.out.println("❌ Store provisioning failed");
                return;
            }
            
            // Test 2: Get current profile name
            System.out.println("\n2. Testing storeGetProfileName...");
            try {
                String currentProfile = AskarNative.storeGetProfileName(storeHandle);
                if (currentProfile != null) {
                    System.out.println("✅ Current profile name: " + currentProfile);
                } else {
                    System.out.println("❌ Could not get profile name");
                }
            } catch (Exception e) {
                System.out.println("❌ storeGetProfileName failed: " + e.getMessage());
            }
            
            // Test 3: Get default profile
            System.out.println("\n3. Testing storeGetDefaultProfile...");
            try {
                String defaultProfile = AskarNative.storeGetDefaultProfile(storeHandle);
                if (defaultProfile != null) {
                    System.out.println("✅ Default profile: " + defaultProfile);
                } else {
                    System.out.println("❌ Could not get default profile");
                }
            } catch (Exception e) {
                System.out.println("❌ storeGetDefaultProfile failed: " + e.getMessage());
            }
            
            // Test 4: List profiles
            System.out.println("\n4. Testing storeListProfiles...");
            try {
                String[] profiles = AskarNative.storeListProfiles(storeHandle);
                if (profiles != null && profiles.length > 0) {
                    System.out.println("✅ Found " + profiles.length + " profiles:");
                    for (int i = 0; i < profiles.length; i++) {
                        System.out.println("   " + (i + 1) + ". " + profiles[i]);
                    }
                } else {
                    System.out.println("⚠️  No profiles found or storeListProfiles returned null");
                }
            } catch (Exception e) {
                System.out.println("❌ storeListProfiles failed: " + e.getMessage());
            }
            
            // Test 5: Create a new profile
            System.out.println("\n5. Testing storeCreateProfile...");
            try {
                String newProfileName = AskarNative.storeCreateProfile(storeHandle, "test_profile");
                if (newProfileName != null) {
                    System.out.println("✅ New profile created: " + newProfileName);
                    
                    // List profiles again to verify
                    String[] profilesAfterCreate = AskarNative.storeListProfiles(storeHandle);
                    if (profilesAfterCreate != null) {
                        System.out.println("   Profiles after creation: " + Arrays.toString(profilesAfterCreate));
                    }
                } else {
                    System.out.println("❌ Failed to create new profile");
                }
            } catch (Exception e) {
                System.out.println("❌ storeCreateProfile failed: " + e.getMessage());
            }
            
            // Test 6: Set default profile
            System.out.println("\n6. Testing storeSetDefaultProfile...");
            try {
                AskarNative.storeSetDefaultProfile(storeHandle, "test_profile");
                System.out.println("✅ Default profile set to 'test_profile'");
                
                // Verify the change
                String newDefaultProfile = AskarNative.storeGetDefaultProfile(storeHandle);
                if ("test_profile".equals(newDefaultProfile)) {
                    System.out.println("✅ Default profile verified: " + newDefaultProfile);
                } else {
                    System.out.println("⚠️  Default profile verification failed, got: " + newDefaultProfile);
                }
            } catch (Exception e) {
                System.out.println("❌ storeSetDefaultProfile failed: " + e.getMessage());
            }
            
            // Test 7: Store copy to new location
            System.out.println("\n7. Testing storeCopyTo...");
            try {
                long copiedStoreHandle = AskarNative.storeCopyTo(storeHandle, copyUri, "raw", "copy_password", true);
                if (copiedStoreHandle != 0) {
                    System.out.println("✅ Store copied successfully, new handle: " + copiedStoreHandle);
                    
                    // Verify the copied store
                    String copiedProfileName = AskarNative.storeGetProfileName(copiedStoreHandle);
                    System.out.println("   Copied store profile: " + copiedProfileName);
                    
                    // Close the copied store
                    AskarNative.storeClose(copiedStoreHandle);
                    System.out.println("✅ Copied store closed");
                } else {
                    System.out.println("❌ Store copy failed");
                }
            } catch (Exception e) {
                System.out.println("❌ storeCopyTo failed: " + e.getMessage());
            }
            
            // Test 8: Store rekey (change password)
            System.out.println("\n8. Testing storeRekey...");
            try {
                AskarNative.storeRekey(storeHandle, "raw", "new_password");
                System.out.println("✅ Store rekeyed successfully with new password");
            } catch (Exception e) {
                System.out.println("❌ storeRekey failed: " + e.getMessage());
            }
            
            // Test 9: Remove profile
            System.out.println("\n9. Testing storeRemoveProfile...");
            try {
                boolean profileRemoved = AskarNative.storeRemoveProfile(storeHandle, "test_profile");
                if (profileRemoved) {
                    System.out.println("✅ Profile 'test_profile' removed successfully");
                    
                    // Verify removal by listing profiles
                    String[] profilesAfterRemoval = AskarNative.storeListProfiles(storeHandle);
                    if (profilesAfterRemoval != null) {
                        System.out.println("   Profiles after removal: " + Arrays.toString(profilesAfterRemoval));
                    }
                } else {
                    System.out.println("⚠️  Profile 'test_profile' was not removed (might not exist)");
                }
            } catch (Exception e) {
                System.out.println("❌ storeRemoveProfile failed: " + e.getMessage());
            }
            
            // Close the original store before removal test
            AskarNative.storeClose(storeHandle);
            System.out.println("\n✅ Original store closed");
            
            // Test 10: Store remove (delete entire store)
            System.out.println("\n10. Testing storeRemove...");
            try {
                boolean storeRemoved = AskarNative.storeRemove(originalUri);
                if (storeRemoved) {
                    System.out.println("✅ Store removed successfully");
                    
                    // Verify the file is gone
                    File dbFile = new File(originalDbPath);
                    if (!dbFile.exists()) {
                        System.out.println("✅ Store file deleted from filesystem");
                    } else {
                        System.out.println("⚠️  Store file still exists on filesystem");
                    }
                } else {
                    System.out.println("⚠️  Store was not removed (might not exist)");
                }
            } catch (Exception e) {
                System.out.println("❌ storeRemove failed: " + e.getMessage());
            }
            
            // Test 11: Try to remove copied store as well
            System.out.println("\n11. Testing storeRemove for copied store...");
            try {
                boolean copyStoreRemoved = AskarNative.storeRemove(copyUri);
                if (copyStoreRemoved) {
                    System.out.println("✅ Copied store removed successfully");
                } else {
                    System.out.println("⚠️  Copied store was not removed");
                }
            } catch (Exception e) {
                System.out.println("❌ storeRemove for copied store failed: " + e.getMessage());
            }
            
            // Final cleanup
            cleanupFiles(originalDbPath, copyDbPath);
            
            System.out.println("\n🎉 Store Management Test completed!");
            
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void cleanupFiles(String... filePaths) {
        for (String filePath : filePaths) {
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
                System.out.println("Cleaned up file: " + filePath);
            }
        }
    }
}