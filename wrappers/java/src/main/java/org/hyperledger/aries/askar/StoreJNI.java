package org.hyperledger.aries.askar;

import java.io.Closeable;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.hyperledger.aries.askar.Entry;

/**
 * JNI-based implementation of Askar Store functionality.
 * This replaces the JNA-based implementation with direct JNI calls.
 */
public class StoreJNI implements Closeable {
    
    private long handle;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        AskarNative.setMaxLogLevel(3); // INFO level
    }
    
    private StoreJNI(long handle) {
        this.handle = handle;
    }
    
    /**
     * Provision a new store.
     */
    public static StoreJNI provision(String uri, String keyMethod, String passKey, String profile, boolean recreate) 
            throws AskarException {
        try {
            long storeHandle = AskarNative.storeProvision(uri, keyMethod, passKey, profile, recreate);
            if (storeHandle == 0) {
                throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Failed to provision store");
            }
            return new StoreJNI(storeHandle);
        } catch (Exception e) {
            if (e instanceof AskarException) {
                throw e;
            }
            throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Store provision failed", e);
        }
    }
    
    /**
     * Open an existing store.
     */
    public static StoreJNI open(String uri, String keyMethod, String passKey, String profile) 
            throws AskarException {
        try {
            long storeHandle = AskarNative.storeOpen(uri, keyMethod, passKey, profile);
            if (storeHandle == 0) {
                throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Failed to open store");
            }
            return new StoreJNI(storeHandle);
        } catch (Exception e) {
            if (e instanceof AskarException) {
                throw e;
            }
            throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Store open failed", e);
        }
    }
    
    /**
     * Create a new session.
     */
    public SessionJNI session() {
        return new SessionJNI(this.handle);
    }
    
    @Override
    public void close() {
        if (handle != 0) {
            try {
                AskarNative.storeClose(handle);
            } catch (Exception e) {
                // Log error but don't throw in close()
                System.err.println("Error closing store: " + e.getMessage());
            } finally {
                handle = 0;
            }
        }
    }
    
    /**
     * Get the version of the Askar library.
     */
    public static String getVersion() {
        return AskarNative.getVersion();
    }
    
    /**
     * JNI-based Session implementation.
     */
    public static class SessionJNI implements Closeable {
        private long storeHandle;
        private long sessionHandle;
        
        SessionJNI(long storeHandle) {
            this.storeHandle = storeHandle;
        }
        
        /**
         * Open a new session.
         */
        public SessionJNI open() throws AskarException {
            return open(false);
        }
        
        /**
         * Open a new session.
         */
        public SessionJNI open(boolean asTransaction) throws AskarException {
            try {
                this.sessionHandle = AskarNative.sessionStart(storeHandle, null, asTransaction);
                if (this.sessionHandle == 0) {
                    throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Failed to start session");
                }
                return this;
            } catch (Exception e) {
                if (e instanceof AskarException) {
                    throw e;
                }
                throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Session start failed", e);
            }
        }
        
        /**
         * Count entries in a category.
         */
        public int count(String category, String tagFilter) throws AskarException {
            checkOpen();
            try {
                return AskarNative.sessionCount(sessionHandle, category, tagFilter);
            } catch (Exception e) {
                if (e instanceof AskarException) {
                    throw e;
                }
                throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Session count failed", e);
            }
        }
        
        /**
         * Insert a new entry.
         */
        public void insert(String category, String name, byte[] value, Map<String, Object> tags, Long expiryMs) 
                throws AskarException {
            update(EntryOperation.INSERT, category, name, value, tags, expiryMs);
        }
        
        /**
         * Update an entry.
         */
        private void update(EntryOperation operation, String category, String name, byte[] value, 
                           Map<String, Object> tags, Long expiryMs) throws AskarException {
            checkOpen();
            try {
                String tagsJson = null;
                if (tags != null && !tags.isEmpty()) {
                    tagsJson = objectMapper.writeValueAsString(tags);
                }
                
                AskarNative.sessionUpdate(
                    sessionHandle, 
                    operation.getValue(), 
                    category, 
                    name, 
                    value, 
                    tagsJson, 
                    expiryMs != null ? expiryMs : -1
                );
            } catch (JsonProcessingException e) {
                throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Failed to serialize tags", e);
            } catch (Exception e) {
                if (e instanceof AskarException) {
                    throw e;
                }
                throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Session update failed", e);
            }
        }
        
        /**
         * Fetch a single entry.
         */
        public Entry fetch(String category, String name, boolean forUpdate) throws AskarException {
            checkOpen();
            try {
                long entryListHandle = AskarNative.sessionFetch(sessionHandle, category, name, forUpdate);
                
                if (entryListHandle == 0) {
                    return null; // Entry not found
                }
                
                // Process the single entry result
                List<Entry> entries = processEntryList(entryListHandle);
                return entries.isEmpty() ? null : entries.get(0);
            } catch (Exception e) {
                if (e instanceof AskarException) {
                    throw e;
                }
                throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Session fetch failed", e);
            }
        }
        
        /**
         * Fetch all entries from a category.
         */
        public List<Entry> fetchAll(String category, String tagFilter, Integer limit, String orderBy, 
                                  boolean descending, boolean forUpdate) throws AskarException {
            checkOpen();
            try {
                long entryListHandle = AskarNative.sessionFetchAll(
                    sessionHandle, category, tagFilter, 
                    limit != null ? limit : -1, orderBy, descending, forUpdate
                );
                
                if (entryListHandle == 0) {
                    return new ArrayList<>();
                }
                
                return processEntryList(entryListHandle);
            } catch (Exception e) {
                if (e instanceof AskarException) {
                    throw e;
                }
                throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Session fetch all failed", e);
            }
        }
        
        /**
         * Process entry list returned from native code.
         */
        private List<Entry> processEntryList(long entryListHandle) throws AskarException {
            List<Entry> entries = new ArrayList<>();
            
            try {
                int count = AskarNative.entryListCount(entryListHandle);
                
                for (int i = 0; i < count; i++) {
                    String category = AskarNative.entryListGetCategory(entryListHandle, i);
                    String name = AskarNative.entryListGetName(entryListHandle, i);
                    byte[] value = AskarNative.entryListGetValue(entryListHandle, i);
                    String tagsJson = AskarNative.entryListGetTags(entryListHandle, i);
                    
                    Map<String, Object> tags = null;
                    if (tagsJson != null && !tagsJson.isEmpty()) {
                        try {
                            tags = objectMapper.readValue(tagsJson, Map.class);
                        } catch (JsonProcessingException e) {
                            // Log warning and continue with null tags
                            System.err.println("Warning: Failed to parse tags JSON: " + e.getMessage());
                        }
                    }
                    
                    entries.add(new Entry(category, name, value, tags));
                }
                
                return entries;
            } finally {
                // Free the entry list
                AskarNative.entryListFree(entryListHandle);
            }
        }
        
        private void checkOpen() throws AskarException {
            if (sessionHandle == 0) {
                throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Session is not open");
            }
        }
        
        @Override
        public void close() {
            if (sessionHandle != 0) {
                try {
                    AskarNative.sessionClose(sessionHandle, true); // commit = true
                } catch (Exception e) {
                    // Log error but don't throw in close()
                    System.err.println("Error closing session: " + e.getMessage());
                } finally {
                    sessionHandle = 0;
                }
            }
        }
    }
}