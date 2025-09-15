package org.hyperledger.aries.askar;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a secure storage instance.
 */
public class Store implements AutoCloseable {

    private long handle;
    private final String uri;
    private boolean closed = false;

    Store(long handle, String uri) {
        this.handle = handle;
        this.uri = uri;
        LibraryLoader.ensureInitialized();
    }

    /**
     * Provision a new store.
     * @param uri The store URI
     * @param keyMethod The key derivation method (optional)
     * @param passKey The passkey (optional)
     * @param profile The profile name (optional)
     * @param recreate Whether to recreate if exists
     * @return A new Store instance
     * @throws AskarException If provisioning fails
     */
    public static Store provision(String uri, String keyMethod, String passKey, String profile, boolean recreate) throws AskarException {
        LibraryLoader.ensureInitialized();

        CompletableFuture<Pointer> future = LibraryLoader.createCallback();
        long callbackId = LibraryLoader.getNextCallbackId();
        LibraryLoader.registerCallback(callbackId, future);

        int result = AskarLibrary.INSTANCE.askar_store_provision(
                uri,
                keyMethod,
                passKey,
                profile,
                (byte) (recreate ? 1 : 0),
                LibraryLoader.CALLBACK,
                callbackId
        );

        LibraryLoader.checkError(result);

        try {
            Pointer resultPtr = future.get();
            return new Store(Pointer.nativeValue(resultPtr), uri);
        } catch (Exception e) {
            throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Async operation failed", e);
        }
    }

    /**
     * Open an existing store.
     * @param uri The store URI
     * @param keyMethod The key derivation method (optional)
     * @param passKey The passkey (optional)
     * @param profile The profile name (optional)
     * @return A Store instance
     * @throws AskarException If opening fails
     */
    public static Store open(String uri, String keyMethod, String passKey, String profile) throws AskarException {
        LibraryLoader.ensureInitialized();

        CompletableFuture<Pointer> future = LibraryLoader.createCallback();
        long callbackId = LibraryLoader.getNextCallbackId();
        LibraryLoader.registerCallback(callbackId, future);

        int result = AskarLibrary.INSTANCE.askar_store_open(
                uri,
                keyMethod,
                passKey,
                profile,
                LibraryLoader.CALLBACK,
                callbackId
        );

        LibraryLoader.checkError(result);

        try {
            Pointer resultPtr = future.get();
            return new Store(Pointer.nativeValue(resultPtr), uri);
        } catch (Exception e) {
            throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Async operation failed", e);
        }
    }

    /**
     * Remove a store completely.
     * @param uri The store URI
     * @return true if the store was removed
     * @throws AskarException If removal fails
     */
    public static boolean remove(String uri) throws AskarException {
        LibraryLoader.ensureInitialized();

        CompletableFuture<Pointer> future = LibraryLoader.createCallback();
        long callbackId = LibraryLoader.getNextCallbackId();
        LibraryLoader.registerCallback(callbackId, future);

        int result = AskarLibrary.INSTANCE.askar_store_remove(
                uri,
                LibraryLoader.CALLBACK,
                callbackId
        );

        LibraryLoader.checkError(result);

        try {
            Pointer resultPtr = future.get();
            return resultPtr.getByte(0) != 0;
        } catch (Exception e) {
            throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Async operation failed", e);
        }
    }

    /**
     * Generate a raw key for store encryption.
     * @param seed Optional seed for key generation
     * @return The generated raw key
     * @throws AskarException If generation fails
     */
    public static String generateRawKey(byte[] seed) throws AskarException {
        LibraryLoader.ensureInitialized();
        PointerByReference rawKeyRef = new PointerByReference();

        AskarLibrary.RawBuffer seedBuffer = seed != null ? LibraryLoader.bytesToRawBuffer(seed) : new AskarLibrary.RawBuffer();
        try {
            int result = AskarLibrary.INSTANCE.askar_store_generate_raw_key(seedBuffer, rawKeyRef);
            LibraryLoader.checkError(result);

            return rawKeyRef.getValue().getString(0);
        } finally {
            if (seed != null) {
                LibraryLoader.freeBuffer(seedBuffer);
            }
            LibraryLoader.freeString(rawKeyRef.getValue());
        }
    }

    /**
     * Get the store URI.
     * @return The URI
     */
    public String getUri() {
        return uri;
    }

    /**
     * Create a new profile in the store.
     * @param profileName The profile name (optional, will be generated if null)
     * @return The actual profile name created
     * @throws AskarException If creation fails
     */
    public String createProfile(String profileName) throws AskarException {
        checkClosed();

        CompletableFuture<Pointer> future = LibraryLoader.createCallback();
        long callbackId = LibraryLoader.getNextCallbackId();
        LibraryLoader.registerCallback(callbackId, future);

        int result = AskarLibrary.INSTANCE.askar_store_create_profile(
                handle,
                profileName,
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
     * Get the current profile name.
     * @return The current profile name
     * @throws AskarException If operation fails
     */
    public String getProfileName() throws AskarException {
        checkClosed();

        CompletableFuture<Pointer> future = LibraryLoader.createCallback();
        long callbackId = LibraryLoader.getNextCallbackId();
        LibraryLoader.registerCallback(callbackId, future);

        int result = AskarLibrary.INSTANCE.askar_store_get_profile_name(
                handle,
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
     * Get the default profile name.
     * @return The default profile name
     * @throws AskarException If operation fails
     */
    public String getDefaultProfile() throws AskarException {
        checkClosed();

        CompletableFuture<Pointer> future = LibraryLoader.createCallback();
        long callbackId = LibraryLoader.getNextCallbackId();
        LibraryLoader.registerCallback(callbackId, future);

        int result = AskarLibrary.INSTANCE.askar_store_get_default_profile(
                handle,
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
     * Set the default profile.
     * @param profileName The profile name to set as default
     * @throws AskarException If operation fails
     */
    public void setDefaultProfile(String profileName) throws AskarException {
        checkClosed();

        CompletableFuture<Pointer> future = LibraryLoader.createCallback();
        long callbackId = LibraryLoader.getNextCallbackId();
        LibraryLoader.registerCallback(callbackId, future);

        int result = AskarLibrary.INSTANCE.askar_store_set_default_profile(
                handle,
                profileName,
                LibraryLoader.CALLBACK,
                callbackId
        );

        LibraryLoader.checkError(result);

        try {
            future.get(); // Wait for completion
        } catch (Exception e) {
            throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Async operation failed", e);
        }
    }

    /**
     * List all profiles in the store.
     * @return List of profile names
     * @throws AskarException If operation fails
     */
    public List<String> listProfiles() throws AskarException {
        checkClosed();

        CompletableFuture<Pointer> future = LibraryLoader.createCallback();
        long callbackId = LibraryLoader.getNextCallbackId();
        LibraryLoader.registerCallback(callbackId, future);

        int result = AskarLibrary.INSTANCE.askar_store_list_profiles(
                handle,
                LibraryLoader.CALLBACK,
                callbackId
        );

        LibraryLoader.checkError(result);

        try {
            Pointer resultPtr = future.get();
            return parseStringList(Pointer.nativeValue(resultPtr));
        } catch (Exception e) {
            throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Async operation failed", e);
        }
    }

    /**
     * Remove a profile from the store.
     * @param profileName The profile name to remove
     * @return true if the profile was removed
     * @throws AskarException If operation fails
     */
    public boolean removeProfile(String profileName) throws AskarException {
        checkClosed();

        CompletableFuture<Pointer> future = LibraryLoader.createCallback();
        long callbackId = LibraryLoader.getNextCallbackId();
        LibraryLoader.registerCallback(callbackId, future);

        int result = AskarLibrary.INSTANCE.askar_store_remove_profile(
                handle,
                profileName,
                LibraryLoader.CALLBACK,
                callbackId
        );

        LibraryLoader.checkError(result);

        try {
            Pointer resultPtr = future.get();
            return resultPtr.getByte(0) != 0;
        } catch (Exception e) {
            throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Async operation failed", e);
        }
    }

    /**
     * Rename a profile.
     * @param fromProfile The current profile name
     * @param toProfile The new profile name
     * @return true if the profile was renamed
     * @throws AskarException If operation fails
     */
    public boolean renameProfile(String fromProfile, String toProfile) throws AskarException {
        checkClosed();

        CompletableFuture<Pointer> future = LibraryLoader.createCallback();
        long callbackId = LibraryLoader.getNextCallbackId();
        LibraryLoader.registerCallback(callbackId, future);

        int result = AskarLibrary.INSTANCE.askar_store_rename_profile(
                handle,
                fromProfile,
                toProfile,
                LibraryLoader.CALLBACK,
                callbackId
        );

        LibraryLoader.checkError(result);

        try {
            Pointer resultPtr = future.get();
            return resultPtr.getByte(0) != 0;
        } catch (Exception e) {
            throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Async operation failed", e);
        }
    }

    /**
     * Change the store's master key.
     * @param keyMethod The new key derivation method (optional)
     * @param passKey The new passkey (optional)
     * @throws AskarException If operation fails
     */
    public void rekey(String keyMethod, String passKey) throws AskarException {
        checkClosed();

        CompletableFuture<Pointer> future = LibraryLoader.createCallback();
        long callbackId = LibraryLoader.getNextCallbackId();
        LibraryLoader.registerCallback(callbackId, future);

        int result = AskarLibrary.INSTANCE.askar_store_rekey(
                handle,
                keyMethod,
                passKey,
                LibraryLoader.CALLBACK,
                callbackId
        );

        LibraryLoader.checkError(result);

        try {
            future.get(); // Wait for completion
        } catch (Exception e) {
            throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Async operation failed", e);
        }
    }

    /**
     * Copy the store to a new location.
     * @param targetUri The target store URI
     * @param keyMethod The key derivation method for the target (optional)
     * @param passKey The passkey for the target (optional)
     * @param recreate Whether to recreate the target if it exists
     * @return A new Store instance for the copy
     * @throws AskarException If operation fails
     */
    public Store copyTo(String targetUri, String keyMethod, String passKey, boolean recreate) throws AskarException {
        checkClosed();

        CompletableFuture<Pointer> future = LibraryLoader.createCallback();
        long callbackId = LibraryLoader.getNextCallbackId();
        LibraryLoader.registerCallback(callbackId, future);

        int result = AskarLibrary.INSTANCE.askar_store_copy(
                handle,
                targetUri,
                keyMethod,
                passKey,
                (byte) (recreate ? 1 : 0),
                LibraryLoader.CALLBACK,
                callbackId
        );

        LibraryLoader.checkError(result);

        try {
            Pointer resultPtr = future.get();
            return new Store(Pointer.nativeValue(resultPtr), targetUri);
        } catch (Exception e) {
            throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Async operation failed", e);
        }
    }

    /**
     * Open a new session (non-transactional).
     * @param profile The profile to use (optional)
     * @return A SessionBuilder to configure and open the session
     */
    public SessionBuilder session(String profile) {
        return new SessionBuilder(handle, profile, false);
    }

    /**
     * Open a new session (non-transactional) with default profile.
     * @return A SessionBuilder to configure and open the session
     */
    public SessionBuilder session() {
        return session(null);
    }

    /**
     * Open a new transaction session.
     * @param profile The profile to use (optional)
     * @return A SessionBuilder to configure and open the transaction
     */
    public SessionBuilder transaction(String profile) {
        return new SessionBuilder(handle, profile, true);
    }

    /**
     * Open a new transaction session with default profile.
     * @return A SessionBuilder to configure and open the transaction
     */
    public SessionBuilder transaction() {
        return transaction(null);
    }

    /**
     * Builder for configuring and opening sessions.
     */
    public static class SessionBuilder {
        private final long storeHandle;
        private final String profile;
        private final boolean isTransaction;
        private boolean autocommit = false;

        SessionBuilder(long storeHandle, String profile, boolean isTransaction) {
            this.storeHandle = storeHandle;
            this.profile = profile;
            this.isTransaction = isTransaction;
        }

        /**
         * Set autocommit mode for transactions.
         * @param autocommit true to enable autocommit
         * @return This builder
         */
        public SessionBuilder autocommit(boolean autocommit) {
            this.autocommit = autocommit;
            return this;
        }

        /**
         * Open the session.
         * @return A new Session instance
         * @throws AskarException If opening fails
         */
        public Session open() throws AskarException {
            LibraryLoader.ensureInitialized();

            CompletableFuture<Pointer> future = LibraryLoader.createCallback();
            long callbackId = LibraryLoader.getNextCallbackId();
            LibraryLoader.registerCallback(callbackId, future);

            int result = AskarLibrary.INSTANCE.askar_session_start(
                    storeHandle,
                    profile,
                    (byte) (isTransaction ? 1 : 0),
                    LibraryLoader.CALLBACK,
                    callbackId
            );

            LibraryLoader.checkError(result);

            try {
                Pointer resultPtr = future.get();
                return new Session(Pointer.nativeValue(resultPtr), isTransaction, autocommit);
            } catch (Exception e) {
                throw new AskarException(AskarException.ErrorCode.UNEXPECTED, "Async operation failed", e);
            }
        }
    }

    private List<String> parseStringList(long stringListHandle) {
        List<String> strings = new ArrayList<>();

        int count = AskarLibrary.INSTANCE.askar_string_list_count(stringListHandle);

        for (int i = 0; i < count; i++) {
            PointerByReference itemRef = new PointerByReference();
            AskarLibrary.INSTANCE.askar_string_list_get_item(stringListHandle, i, itemRef);

            try {
                strings.add(itemRef.getValue().getString(0));
            } finally {
                LibraryLoader.freeString(itemRef.getValue());
            }
        }

        AskarLibrary.INSTANCE.askar_string_list_free(stringListHandle);
        return strings;
    }

    private void checkClosed() throws AskarException {
        if (closed) {
            throw new AskarException(AskarException.ErrorCode.WRAPPER, "Store has been closed");
        }
    }

    @Override
    public void close() {
        close(false);
    }

    /**
     * Close the store.
     * @param remove Whether to remove the store completely
     * @return true if the store was removed (only when remove=true)
     */
    public boolean close(boolean remove) {
        if (!closed && handle != 0) {
            try {
                CompletableFuture<Pointer> future = LibraryLoader.createCallback();
                long callbackId = LibraryLoader.getNextCallbackId();
                LibraryLoader.registerCallback(callbackId, future);

                int result = AskarLibrary.INSTANCE.askar_store_close(
                        handle,
                        LibraryLoader.CALLBACK,
                        callbackId
                );

                LibraryLoader.checkError(result);
                future.get(); // Wait for completion

                handle = 0;
                closed = true;

                if (remove) {
                    return Store.remove(uri);
                }
            } catch (Exception e) {
                // Log error but continue with cleanup
            }
        }
        return false;
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}