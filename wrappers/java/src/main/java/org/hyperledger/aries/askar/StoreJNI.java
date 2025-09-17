package org.hyperledger.aries.askar;

import java.io.Closeable;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * JNI-based implementation of Askar Store functionality.
 * This replaces the JNA-based implementation with direct JNI calls.
 * No external dependencies required.
 */
public class StoreJNI implements Closeable {
    
    private long handle;
    
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
     * Get the store handle.
     */
    public long getHandle() {
        return handle;
    }
    
    /**
     * Create a session for this store.
     */
    public SessionBuilder session() {
        return new SessionBuilder(this);
    }
    
    /**
     * Get the Askar library version.
     */
    public static String getVersion() {
        return AskarNative.getVersion();
    }
    
    @Override
    public void close() {
        if (handle != 0) {
            try {
                AskarNative.storeClose(handle);
            } catch (Exception e) {
                // Can't throw exception from close() per Closeable interface
                // Log error if needed
                System.err.println("Failed to close store: " + e.getMessage());
            } finally {
                handle = 0;
            }
        }
    }
    
    /**
     * Builder for creating sessions.
     */
    public static class SessionBuilder {
        private final StoreJNI store;
        private String profile = "default";
        private boolean asTransaction = false;
        
        SessionBuilder(StoreJNI store) {
            this.store = store;
        }
        
        public SessionBuilder profile(String profile) {
            this.profile = profile;
            return this;
        }
        
        public SessionBuilder asTransaction(boolean asTransaction) {
            this.asTransaction = asTransaction;
            return this;
        }
        
        public SessionJNI open() throws AskarException {
            return new SessionJNI(store.handle, profile, asTransaction);
        }
    }
    
    /**
     * JNI-based session implementation.
     */
    public static class SessionJNI implements Closeable {
        private long sessionHandle;
        
        SessionJNI(long storeHandle, String profile, boolean asTransaction) throws AskarException {
            try {
                this.sessionHandle = AskarNative.sessionStart(storeHandle, profile, asTransaction);
                if (this.sessionHandle == 0) {
                    throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Failed to start session");
                }
            } catch (Exception e) {
                if (e instanceof AskarException) {
                    throw e;
                }
                throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Session start failed", e);
            }
        }
        
        /**
         * Get the session handle.
         */
        public long getHandle() {
            return sessionHandle;
        }
        
        /**
         * Insert data into the store.
         */
        public void insert(String category, String name, byte[] value, Map<String, Object> tags, Long expiryMs) 
                throws AskarException {
            update(EntryOperation.INSERT, category, name, value, tags, expiryMs);
        }
        
        /**
         * Update data in the store.
         */
        public void update(EntryOperation operation, String category, String name, byte[] value, 
                Map<String, Object> tags, Long expiryMs) throws AskarException {
            checkOpen();
            try {
                String tagsJson = null;
                if (tags != null && !tags.isEmpty()) {
                    tagsJson = mapToSimpleJson(tags);
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
         * Fetch all entries matching criteria.
         */
        public List<Entry> fetchAll(String category, String tagFilter, Integer limit, 
                String orderBy, Boolean descending, Boolean forUpdate) throws AskarException {
            checkOpen();
            try {
                long entryListHandle = AskarNative.sessionFetchAll(
                    sessionHandle, 
                    category, 
                    tagFilter, 
                    limit != null ? limit : -1, 
                    orderBy, 
                    descending != null ? descending : false, 
                    forUpdate != null ? forUpdate : false
                );
                
                if (entryListHandle == 0) {
                    return new ArrayList<>(); // No entries found
                }
                
                return processEntryList(entryListHandle);
            } catch (Exception e) {
                if (e instanceof AskarException) {
                    throw e;
                }
                throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Session fetchAll failed", e);
            }
        }
        
        /**
         * Count entries matching criteria.
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
        
        private List<Entry> processEntryList(long entryListHandle) throws AskarException {
            try {
                int count = AskarNative.entryListCount(entryListHandle);
                List<Entry> entries = new ArrayList<>(count);
                
                for (int i = 0; i < count; i++) {
                    String category = AskarNative.entryListGetCategory(entryListHandle, i);
                    String name = AskarNative.entryListGetName(entryListHandle, i);
                    byte[] value = AskarNative.entryListGetValue(entryListHandle, i);
                    String tagsJson = AskarNative.entryListGetTags(entryListHandle, i);
                    
                    Map<String, Object> tags = null;
                    if (tagsJson != null && !tagsJson.isEmpty()) {
                        tags = parseSimpleJson(tagsJson);
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
                    AskarNative.sessionClose(sessionHandle, true);
                } catch (Exception e) {
                    // Can't throw exception from close() per Closeable interface
                    // Log error if needed
                    System.err.println("Failed to close session: " + e.getMessage());
                } finally {
                    sessionHandle = 0;
                }
            }
        }
    }
    
    /**
     * Simple JSON serialization for tags (no external dependencies).
     * This is a basic implementation for simple key-value pairs.
     */
    private static String mapToSimpleJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append("\"").append(escapeJsonString(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(escapeJsonString((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append("\"").append(escapeJsonString(String.valueOf(value))).append("\"");
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Simple JSON parsing for tags (no external dependencies).
     * This is a basic implementation for simple key-value pairs.
     */
    private static Map<String, Object> parseSimpleJson(String json) {
        Map<String, Object> result = new HashMap<>();
        if (json == null || json.trim().isEmpty() || "{}".equals(json.trim())) {
            return result;
        }
        
        // Very basic JSON parsing - sufficient for simple tag structures
        String content = json.trim();
        if (content.startsWith("{") && content.endsWith("}")) {
            content = content.substring(1, content.length() - 1);
            if (!content.trim().isEmpty()) {
                String[] pairs = content.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":", 2);
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim().replaceAll("^\"|\"$", "");
                        String value = keyValue[1].trim().replaceAll("^\"|\"$", "");
                        result.put(key, value);
                    }
                }
            }
        }
        
        return result;
    }
    
    private static String escapeJsonString(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}