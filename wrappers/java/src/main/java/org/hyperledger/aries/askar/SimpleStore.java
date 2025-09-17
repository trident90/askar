package org.hyperledger.aries.askar;

import java.io.Closeable;

/**
 * Simplified high-level wrapper for Askar store operations.
 * Uses only existing native methods and provides basic convenience methods.
 */
public class SimpleStore implements Closeable {
    
    private final StoreJNI storeJNI;
    
    private SimpleStore(StoreJNI storeJNI) {
        this.storeJNI = storeJNI;
    }
    
    /**
     * Provision a new store with the specified configuration.
     * 
     * @param uri Database URI (e.g., "sqlite:///path/to/db.sqlite")
     * @param keyMethod Key derivation method ("raw", "kdf:argon2i:mod", etc.)
     * @param passKey Password or key for encryption (null for no encryption)
     * @param profile Store profile name (null for default)
     * @param recreate Whether to recreate if store exists
     * @return New SimpleStore instance
     * @throws AskarException if provisioning fails
     */
    public static SimpleStore provision(String uri, String keyMethod, String passKey, String profile, boolean recreate) 
            throws AskarException {
        StoreJNI storeJNI = StoreJNI.provision(uri, keyMethod, passKey, profile, recreate);
        return new SimpleStore(storeJNI);
    }
    
    /**
     * Open an existing store.
     * 
     * @param uri Database URI
     * @param keyMethod Key derivation method
     * @param passKey Password or key for decryption
     * @param profile Store profile name (null for default)
     * @return SimpleStore instance
     * @throws AskarException if opening fails
     */
    public static SimpleStore open(String uri, String keyMethod, String passKey, String profile) 
            throws AskarException {
        StoreJNI storeJNI = StoreJNI.open(uri, keyMethod, passKey, profile);
        return new SimpleStore(storeJNI);
    }
    
    /**
     * Get the Askar library version.
     * 
     * @return Version string
     */
    public static String getVersion() {
        return StoreJNI.getVersion();
    }
    
    /**
     * Create a simple session for this store.
     * 
     * @return SimpleSession instance
     * @throws AskarException if session creation fails
     */
    public SimpleSession createSession() throws AskarException {
        StoreJNI.SessionJNI sessionJNI = storeJNI.session().open();
        return new SimpleSession(sessionJNI);
    }
    
    /**
     * Create a transaction session for this store.
     * 
     * @return SimpleSession instance in transaction mode
     * @throws AskarException if session creation fails
     */
    public SimpleSession createTransaction() throws AskarException {
        StoreJNI.SessionJNI sessionJNI = storeJNI.session().asTransaction(true).open();
        return new SimpleSession(sessionJNI);
    }
    
    /**
     * Get the underlying StoreJNI handle.
     * 
     * @return StoreJNI instance
     */
    public StoreJNI getStoreJNI() {
        return storeJNI;
    }
    
    @Override
    public void close() {
        if (storeJNI != null) {
            storeJNI.close();
        }
    }
    
    @Override
    public String toString() {
        return String.format("SimpleStore{handle=%d}", storeJNI != null ? storeJNI.getHandle() : 0);
    }
}