package org.hyperledger.aries.askar;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

/**
 * Simplified high-level wrapper for Askar session operations.
 * Uses only existing native methods and provides convenient overloads.
 */
public class SimpleSession implements Closeable {
    
    private final StoreJNI.SessionJNI sessionJNI;
    
    SimpleSession(StoreJNI.SessionJNI sessionJNI) {
        this.sessionJNI = sessionJNI;
    }
    
    // ==================== Data Operations ====================
    
    /**
     * Insert a new entry with string value.
     * 
     * @param category Entry category
     * @param name Entry name/key
     * @param value Entry value as string
     * @throws AskarException if insertion fails
     */
    public void insert(String category, String name, String value) throws AskarException {
        sessionJNI.insert(category, name, value.getBytes(), null, null);
    }
    
    /**
     * Insert a new entry with byte value.
     * 
     * @param category Entry category
     * @param name Entry name/key
     * @param value Entry value as bytes
     * @throws AskarException if insertion fails
     */
    public void insert(String category, String name, byte[] value) throws AskarException {
        sessionJNI.insert(category, name, value, null, null);
    }
    
    /**
     * Insert a new entry with tags.
     * 
     * @param category Entry category
     * @param name Entry name/key
     * @param value Entry value as string
     * @param tags Entry tags
     * @param expiryMs Optional expiry time in milliseconds
     * @throws AskarException if insertion fails
     */
    public void insert(String category, String name, String value, Map<String, Object> tags, Long expiryMs) 
            throws AskarException {
        sessionJNI.insert(category, name, value.getBytes(), tags, expiryMs);
    }
    
    /**
     * Replace an existing entry.
     * 
     * @param category Entry category
     * @param name Entry name/key
     * @param value New value as string
     * @throws AskarException if operation fails
     */
    public void replace(String category, String name, String value) throws AskarException {
        sessionJNI.update(EntryOperation.REPLACE, category, name, value.getBytes(), null, null);
    }
    
    /**
     * Replace an existing entry with tags.
     * 
     * @param category Entry category
     * @param name Entry name/key
     * @param value New value as string
     * @param tags Entry tags
     * @param expiryMs Optional expiry time in milliseconds
     * @throws AskarException if operation fails
     */
    public void replace(String category, String name, String value, Map<String, Object> tags, Long expiryMs) 
            throws AskarException {
        sessionJNI.update(EntryOperation.REPLACE, category, name, value.getBytes(), tags, expiryMs);
    }
    
    /**
     * Remove an entry from the store.
     * 
     * @param category Entry category
     * @param name Entry name/key
     * @throws AskarException if removal fails
     */
    public void remove(String category, String name) throws AskarException {
        sessionJNI.update(EntryOperation.REMOVE, category, name, null, null, null);
    }
    
    /**
     * Fetch a single entry.
     * 
     * @param category Entry category
     * @param name Entry name/key
     * @return Entry or null if not found
     * @throws AskarException if fetch fails
     */
    public Entry fetch(String category, String name) throws AskarException {
        return sessionJNI.fetch(category, name, false);
    }
    
    /**
     * Fetch a single entry for update.
     * 
     * @param category Entry category
     * @param name Entry name/key
     * @return Entry or null if not found
     * @throws AskarException if fetch fails
     */
    public Entry fetchForUpdate(String category, String name) throws AskarException {
        return sessionJNI.fetch(category, name, true);
    }
    
    /**
     * Fetch all entries in a category.
     * 
     * @param category Entry category
     * @return List of entries
     * @throws AskarException if fetch fails
     */
    public List<Entry> fetchAll(String category) throws AskarException {
        return sessionJNI.fetchAll(category, null, null, null, null, null);
    }
    
    /**
     * Fetch all entries with filtering.
     * 
     * @param category Entry category (null for all categories)
     * @param tagFilter Tag filter expression (null for no filtering)
     * @param limit Maximum number of entries to return (null for no limit)
     * @return List of matching entries
     * @throws AskarException if fetch fails
     */
    public List<Entry> fetchAll(String category, String tagFilter, Integer limit) throws AskarException {
        return sessionJNI.fetchAll(category, tagFilter, limit, null, null, null);
    }
    
    /**
     * Count all entries in a category.
     * 
     * @param category Entry category
     * @return Number of entries
     * @throws AskarException if count fails
     */
    public int count(String category) throws AskarException {
        return sessionJNI.count(category, null);
    }
    
    /**
     * Count entries matching criteria.
     * 
     * @param category Entry category (null for all categories)
     * @param tagFilter Tag filter expression (null for no filtering)
     * @return Number of matching entries
     * @throws AskarException if count fails
     */
    public int count(String category, String tagFilter) throws AskarException {
        return sessionJNI.count(category, tagFilter);
    }
    
    // ==================== Key Operations ====================
    
    /**
     * Insert a key into the session.
     * 
     * @param name Key name
     * @param key SimpleKey instance
     * @param metadata Optional metadata
     * @throws AskarException if insertion fails
     */
    public void insertKey(String name, SimpleKey key, String metadata) throws AskarException {
        AskarNative.sessionInsertKey(sessionJNI.getHandle(), key.getHandle(), name, metadata, null, -1);
    }
    
    /**
     * Insert a key with tags.
     * 
     * @param name Key name
     * @param key SimpleKey instance
     * @param metadata Optional metadata
     * @param tags Optional tags as JSON string
     * @throws AskarException if insertion fails
     */
    public void insertKey(String name, SimpleKey key, String metadata, String tags) throws AskarException {
        AskarNative.sessionInsertKey(sessionJNI.getHandle(), key.getHandle(), name, metadata, tags, -1);
    }
    
    /**
     * Fetch a key from the session.
     * 
     * @param name Key name
     * @return SimpleKey instance or null if not found
     * @throws AskarException if fetch fails
     */
    public SimpleKey fetchKey(String name) throws AskarException {
        long keyHandle = AskarNative.sessionFetchKey(sessionJNI.getHandle(), name, false);
        return keyHandle != 0 ? new SimpleKey(keyHandle) : null;
    }
    
    /**
     * Remove a key from the session.
     * 
     * @param name Key name
     * @throws AskarException if removal fails
     */
    public void removeKey(String name) throws AskarException {
        AskarNative.sessionRemoveKey(sessionJNI.getHandle(), name);
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Get the underlying session handle.
     * 
     * @return Session handle
     */
    public long getHandle() {
        return sessionJNI.getHandle();
    }
    
    /**
     * Get the underlying SessionJNI instance.
     * 
     * @return SessionJNI instance
     */
    public StoreJNI.SessionJNI getSessionJNI() {
        return sessionJNI;
    }
    
    @Override
    public void close() {
        if (sessionJNI != null) {
            sessionJNI.close();
        }
    }
    
    @Override
    public String toString() {
        return String.format("SimpleSession{handle=%d}", sessionJNI != null ? sessionJNI.getHandle() : 0);
    }
}