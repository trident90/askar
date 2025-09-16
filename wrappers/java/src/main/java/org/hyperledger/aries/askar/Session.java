package org.hyperledger.aries.askar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a database session for performing operations on the store.
 */
public class Session implements AutoCloseable {

    /**
     * Entry operations for session updates.
     */
    public enum EntryOperation {
        INSERT(1),
        REPLACE(2),
        REMOVE(3);

        private final int value;

        EntryOperation(int value) {
            this.value = value;
        }

        public byte getValue() {
            return (byte) value;
        }
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private long handle;
    private final boolean isTransaction;
    private boolean autocommit;
    private boolean closed = false;

    Session(long handle, boolean isTransaction, boolean autocommit) {
        this.handle = handle;
        this.isTransaction = isTransaction;
        this.autocommit = autocommit;
        LibraryLoader.ensureInitialized();
    }

    /**
     * Check if this session is a transaction.
     * @return true if this is a transaction session
     */
    public boolean isTransaction() {
        return isTransaction;
    }

    /**
     * Check if autocommit is enabled.
     * @return true if autocommit is enabled
     */
    public boolean isAutocommit() {
        return autocommit;
    }

    /**
     * Set autocommit mode.
     * @param autocommit true to enable autocommit
     */
    public void setAutocommit(boolean autocommit) {
        this.autocommit = autocommit;
    }

    /**
     * Count entries matching the criteria.
     * @param category The entry category (optional)
     * @param tagFilter The tag filter as JSON string or Map (optional)
     * @return The count of matching entries
     * @throws AskarException If operation fails
     */
    public int count(String category, Object tagFilter) throws AskarException {
        checkClosed();
        String tagFilterJson = convertTagFilter(tagFilter);

        CompletableFuture<Pointer> future = LibraryLoader.createCallback();
        long callbackId = LibraryLoader.getNextCallbackId();
        LibraryLoader.registerCallback(callbackId, future);

        int result = AskarLibrary.INSTANCE.askar_session_count(
                handle,
                category,
                tagFilterJson,
                LibraryLoader.CALLBACK,
                callbackId
        );

        LibraryLoader.checkError(result);

        try {
            Pointer resultPtr = future.get();
            return resultPtr.getInt(0);
        } catch (Exception e) {
            throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Async operation failed", e);
        }
    }

    /**
     * Fetch a single entry.
     * @param category The entry category
     * @param name The entry name
     * @param forUpdate Whether to lock the entry for update
     * @return The entry or null if not found
     * @throws AskarException If operation fails
     */
    public Entry fetch(String category, String name, boolean forUpdate) throws AskarException {
        checkClosed();

        CompletableFuture<Pointer> future = LibraryLoader.createCallback();
        long callbackId = LibraryLoader.getNextCallbackId();
        LibraryLoader.registerCallback(callbackId, future);

        int result = AskarLibrary.INSTANCE.askar_session_fetch(
                handle,
                category,
                name,
                (byte) (forUpdate ? 1 : 0),
                LibraryLoader.CALLBACK,
                callbackId
        );

        LibraryLoader.checkError(result);

        try {
            Pointer resultPtr = future.get();
            if (resultPtr == null || resultPtr == Pointer.NULL) {
                return null;
            }

            // Parse entry list (should contain exactly one entry)
            List<Entry> entries = parseEntryList(Pointer.nativeValue(resultPtr));
            return entries.isEmpty() ? null : entries.get(0);
        } catch (Exception e) {
            throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Async operation failed", e);
        }
    }

    /**
     * Fetch all entries matching the criteria.
     * @param category The entry category (optional)
     * @param tagFilter The tag filter as JSON string or Map (optional)
     * @param limit Maximum number of entries to return (optional)
     * @param orderBy Field to order by (optional)
     * @param descending Whether to sort in descending order
     * @param forUpdate Whether to lock entries for update
     * @return List of matching entries
     * @throws AskarException If operation fails
     */
    public List<Entry> fetchAll(String category, Object tagFilter, Integer limit, String orderBy, boolean descending, boolean forUpdate) throws AskarException {
        checkClosed();
        String tagFilterJson = convertTagFilter(tagFilter);

        CompletableFuture<Pointer> future = LibraryLoader.createCallback();
        long callbackId = LibraryLoader.getNextCallbackId();
        LibraryLoader.registerCallback(callbackId, future);

        int result = AskarLibrary.INSTANCE.askar_session_fetch_all(
                handle,
                category,
                tagFilterJson,
                limit != null ? limit : -1,
                orderBy,
                (byte) (descending ? 1 : 0),
                (byte) (forUpdate ? 1 : 0),
                LibraryLoader.CALLBACK,
                callbackId
        );

        LibraryLoader.checkError(result);

        try {
            Pointer resultPtr = future.get();
            if (resultPtr == null || resultPtr == Pointer.NULL) {
                return new ArrayList<>();
            }

            return parseEntryList(Pointer.nativeValue(resultPtr));
        } catch (Exception e) {
            throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Async operation failed", e);
        }
    }

    /**
     * Insert a new entry.
     * @param category The entry category
     * @param name The entry name
     * @param value The entry value
     * @param tags The entry tags (optional)
     * @param expiryMs Expiry time in milliseconds (optional)
     * @throws AskarException If operation fails
     */
    public void insert(String category, String name, byte[] value, Map<String, Object> tags, Long expiryMs) throws AskarException {
        update(EntryOperation.INSERT, category, name, value, tags, expiryMs);
    }

    /**
     * Replace an existing entry.
     * @param category The entry category
     * @param name The entry name
     * @param value The entry value
     * @param tags The entry tags (optional)
     * @param expiryMs Expiry time in milliseconds (optional)
     * @throws AskarException If operation fails
     */
    public void replace(String category, String name, byte[] value, Map<String, Object> tags, Long expiryMs) throws AskarException {
        update(EntryOperation.REPLACE, category, name, value, tags, expiryMs);
    }

    /**
     * Remove an entry.
     * @param category The entry category
     * @param name The entry name
     * @throws AskarException If operation fails
     */
    public void remove(String category, String name) throws AskarException {
        update(EntryOperation.REMOVE, category, name, null, null, null);
    }

    private void update(EntryOperation operation, String category, String name, byte[] value, Map<String, Object> tags, Long expiryMs) throws AskarException {
        checkClosed();
        String tagsJson = convertTags(tags);

        CompletableFuture<Pointer> future = LibraryLoader.createCallback();
        long callbackId = LibraryLoader.getNextCallbackId();
        LibraryLoader.registerCallback(callbackId, future);

        AskarLibrary.RawBuffer valueBuffer = value != null ? LibraryLoader.bytesToRawBuffer(value) : new AskarLibrary.RawBuffer();
        try {
            int result = AskarLibrary.INSTANCE.askar_session_update(
                    handle,
                    operation.getValue(),
                    category,
                    name,
                    valueBuffer,
                    tagsJson,
                    expiryMs != null ? expiryMs : -1,
                    LibraryLoader.CALLBACK,
                    callbackId
            );

            LibraryLoader.checkError(result);
            future.get(); // Wait for completion
        } catch (Exception e) {
            throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Async operation failed", e);
        } finally {
            // valueBuffer is Java-allocated, no need to free
        }
    }

    /**
     * Insert a key into the store.
     * @param name The key name
     * @param key The key to insert
     * @param metadata Key metadata (optional)
     * @param tags Key tags (optional)
     * @param expiryMs Expiry time in milliseconds (optional)
     * @return The key identifier
     * @throws AskarException If operation fails
     */
    public String insertKey(String name, Key key, String metadata, Map<String, Object> tags, Long expiryMs) throws AskarException {
        checkClosed();
        String tagsJson = convertTags(tags);

        CompletableFuture<Pointer> future = LibraryLoader.createCallback();
        long callbackId = LibraryLoader.getNextCallbackId();
        LibraryLoader.registerCallback(callbackId, future);

        int result = AskarLibrary.INSTANCE.askar_session_insert_key(
                handle,
                key.getHandle(),
                name,
                metadata,
                tagsJson,
                expiryMs != null ? expiryMs : -1,
                LibraryLoader.CALLBACK,
                callbackId
        );

        LibraryLoader.checkError(result);

        try {
            Pointer resultPtr = future.get();
            return resultPtr.getString(0);
        } catch (Exception e) {
            throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Async operation failed", e);
        }
    }

    /**
     * Fetch a key by name.
     * @param name The key name
     * @param forUpdate Whether to lock the key for update
     * @return The key entry or null if not found
     * @throws AskarException If operation fails
     */
    public KeyEntry fetchKey(String name, boolean forUpdate) throws AskarException {
        checkClosed();

        CompletableFuture<Pointer> future = LibraryLoader.createCallback();
        long callbackId = LibraryLoader.getNextCallbackId();
        LibraryLoader.registerCallback(callbackId, future);

        int result = AskarLibrary.INSTANCE.askar_session_fetch_key(
                handle,
                name,
                (byte) (forUpdate ? 1 : 0),
                LibraryLoader.CALLBACK,
                callbackId
        );

        LibraryLoader.checkError(result);

        try {
            Pointer resultPtr = future.get();
            if (resultPtr == null || resultPtr == Pointer.NULL) {
                return null;
            }

            List<KeyEntry> keyEntries = parseKeyEntryList(Pointer.nativeValue(resultPtr));
            return keyEntries.isEmpty() ? null : keyEntries.get(0);
        } catch (Exception e) {
            throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Async operation failed", e);
        }
    }

    /**
     * Commit the transaction (only for transaction sessions).
     * @throws AskarException If operation fails
     */
    public void commit() throws AskarException {
        if (!isTransaction) {
            throw new AskarException(AskarException.ErrorCode.WRAPPER, "Session is not a transaction");
        }
        close(true);
    }

    /**
     * Rollback the transaction (only for transaction sessions).
     * @throws AskarException If operation fails
     */
    public void rollback() throws AskarException {
        if (!isTransaction) {
            throw new AskarException(AskarException.ErrorCode.WRAPPER, "Session is not a transaction");
        }
        close(false);
    }

    private void close(boolean commit) throws AskarException {
        if (!closed && handle != 0) {
            CompletableFuture<Pointer> future = LibraryLoader.createCallback();
            long callbackId = LibraryLoader.getNextCallbackId();
            LibraryLoader.registerCallback(callbackId, future);

            int result = AskarLibrary.INSTANCE.askar_session_close(
                    handle,
                    (byte) (commit ? 1 : 0),
                    LibraryLoader.CALLBACK,
                    callbackId
            );

            LibraryLoader.checkError(result);

            try {
                future.get(); // Wait for completion
            } catch (Exception e) {
                throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Async operation failed", e);
            }

            handle = 0;
            closed = true;
        }
    }

    private void checkClosed() throws AskarException {
        if (closed) {
            throw new AskarException(AskarException.ErrorCode.WRAPPER, "Session has been closed");
        }
    }

    private String convertTagFilter(Object tagFilter) throws AskarException {
        if (tagFilter == null) {
            return null;
        }
        if (tagFilter instanceof String) {
            return (String) tagFilter;
        }
        if (tagFilter instanceof Map) {
            try {
                return objectMapper.writeValueAsString(tagFilter);
            } catch (JsonProcessingException e) {
                throw new AskarException(AskarException.ErrorCode.INPUT, "Failed to serialize tag filter", e);
            }
        }
        throw new AskarException(AskarException.ErrorCode.INPUT, "Tag filter must be a String or Map");
    }

    private String convertTags(Map<String, Object> tags) throws AskarException {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            throw new AskarException(AskarException.ErrorCode.INPUT, "Failed to serialize tags", e);
        }
    }

    private List<Entry> parseEntryList(long entryListHandle) throws AskarException {
        List<Entry> entries = new ArrayList<>();

        int count = AskarLibrary.INSTANCE.askar_entry_list_count(entryListHandle);

        for (int i = 0; i < count; i++) {
            PointerByReference categoryRef = new PointerByReference();
            PointerByReference nameRef = new PointerByReference();
            AskarLibrary.RawBuffer valueBuffer = new AskarLibrary.RawBuffer();
            PointerByReference tagsRef = new PointerByReference();

            AskarLibrary.INSTANCE.askar_entry_list_get_category(entryListHandle, i, categoryRef);
            AskarLibrary.INSTANCE.askar_entry_list_get_name(entryListHandle, i, nameRef);
            AskarLibrary.INSTANCE.askar_entry_list_get_value(entryListHandle, i, valueBuffer);
            AskarLibrary.INSTANCE.askar_entry_list_get_tags(entryListHandle, i, tagsRef);

            try {
                String category = categoryRef.getValue().getString(0);
                String name = nameRef.getValue().getString(0);

                // valueBuffer is now filled by Rust with the SecretBuffer data
                byte[] value = valueBuffer.toByteArray();

                Map<String, Object> tags = null;
                if (tagsRef.getValue() != null && tagsRef.getValue() != Pointer.NULL) {
                    String tagsJson = tagsRef.getValue().getString(0);
                    if (tagsJson != null && !tagsJson.isEmpty()) {
                        try {
                            tags = objectMapper.readValue(tagsJson, Map.class);
                        } catch (JsonProcessingException e) {
                            // Log warning and continue
                        }
                    }
                }

                entries.add(new Entry(category, name, value, tags));
            } finally {
                LibraryLoader.freeString(categoryRef.getValue());
                LibraryLoader.freeString(nameRef.getValue());
                // valueBuffer contains Rust-allocated data, need to free it properly
                if (valueBuffer.data != null && valueBuffer.data != Pointer.NULL) {
                    AskarLibrary.INSTANCE.askar_buffer_free(valueBuffer);
                }
                LibraryLoader.freeString(tagsRef.getValue());
            }
        }

        AskarLibrary.INSTANCE.askar_entry_list_free(entryListHandle);
        return entries;
    }

    private List<KeyEntry> parseKeyEntryList(long keyEntryListHandle) throws AskarException {
        List<KeyEntry> keyEntries = new ArrayList<>();

        int count = AskarLibrary.INSTANCE.askar_key_entry_list_count(keyEntryListHandle);

        for (int i = 0; i < count; i++) {
            PointerByReference algorithmRef = new PointerByReference();
            PointerByReference nameRef = new PointerByReference();
            PointerByReference metadataRef = new PointerByReference();
            PointerByReference tagsRef = new PointerByReference();
            PointerByReference keyRef = new PointerByReference();

            AskarLibrary.INSTANCE.askar_key_entry_list_get_algorithm(keyEntryListHandle, i, algorithmRef);
            AskarLibrary.INSTANCE.askar_key_entry_list_get_name(keyEntryListHandle, i, nameRef);
            AskarLibrary.INSTANCE.askar_key_entry_list_get_metadata(keyEntryListHandle, i, metadataRef);
            AskarLibrary.INSTANCE.askar_key_entry_list_get_tags(keyEntryListHandle, i, tagsRef);
            AskarLibrary.INSTANCE.askar_key_entry_list_load_key(keyEntryListHandle, i, keyRef);

            try {
                String algorithm = algorithmRef.getValue().getString(0);
                String name = nameRef.getValue().getString(0);
                String metadata = metadataRef.getValue() != Pointer.NULL ? metadataRef.getValue().getString(0) : null;

                Map<String, Object> tags = null;
                if (tagsRef.getValue() != null && tagsRef.getValue() != Pointer.NULL) {
                    String tagsJson = tagsRef.getValue().getString(0);
                    if (tagsJson != null && !tagsJson.isEmpty()) {
                        try {
                            tags = objectMapper.readValue(tagsJson, Map.class);
                        } catch (JsonProcessingException e) {
                            // Log warning and continue
                        }
                    }
                }

                Key key = new Key(Pointer.nativeValue(keyRef.getValue()));
                keyEntries.add(new KeyEntry(algorithm, name, metadata, tags, key));
            } finally {
                LibraryLoader.freeString(algorithmRef.getValue());
                LibraryLoader.freeString(nameRef.getValue());
                LibraryLoader.freeString(metadataRef.getValue());
                LibraryLoader.freeString(tagsRef.getValue());
            }
        }

        AskarLibrary.INSTANCE.askar_key_entry_list_free(keyEntryListHandle);
        return keyEntries;
    }

    @Override
    public void close() {
        try {
            close(autocommit);
        } catch (AskarException e) {
            // Log error but don't throw from close()
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}