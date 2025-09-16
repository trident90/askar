package org.hyperledger.aries.askar;

import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * JNA interface to the native Askar library.
 */
public interface AskarLibrary extends Library {

    AskarLibrary INSTANCE = Native.load("aries_askar", AskarLibrary.class);

    /**
     * Callback for asynchronous operations.
     */
    interface AskarCallback extends Callback {
        void callback(long callbackId, int errorCode, Pointer result);
    }

    // Library management
    Pointer askar_version();
    void askar_string_free(Pointer str);
    void askar_buffer_free(RawBuffer buffer);
    int askar_get_current_error(PointerByReference errorJson);
    void askar_terminate();
    int askar_set_max_log_level(int level);
    int askar_set_custom_logger(Pointer context, Pointer logCallback, Pointer enabledCallback, Pointer flushCallback, int maxLevel);

    // Store operations
    int askar_store_provision(String uri, String keyMethod, String passKey, String profile, byte recreate, AskarCallback callback, long callbackId);
    int askar_store_open(String uri, String keyMethod, String passKey, String profile, AskarCallback callback, long callbackId);
    int askar_store_close(long storeHandle, AskarCallback callback, long callbackId);
    int askar_store_remove(String uri, AskarCallback callback, long callbackId);
    int askar_store_copy(long storeHandle, String targetUri, String keyMethod, String passKey, byte recreate, AskarCallback callback, long callbackId);
    int askar_store_rekey(long storeHandle, String keyMethod, String passKey, AskarCallback callback, long callbackId);

    // Store profile operations
    int askar_store_create_profile(long storeHandle, String profile, AskarCallback callback, long callbackId);
    int askar_store_get_profile_name(long storeHandle, AskarCallback callback, long callbackId);
    int askar_store_get_default_profile(long storeHandle, AskarCallback callback, long callbackId);
    int askar_store_set_default_profile(long storeHandle, String profile, AskarCallback callback, long callbackId);
    int askar_store_list_profiles(long storeHandle, AskarCallback callback, long callbackId);
    int askar_store_remove_profile(long storeHandle, String profile, AskarCallback callback, long callbackId);
    int askar_store_rename_profile(long storeHandle, String fromProfile, String toProfile, AskarCallback callback, long callbackId);

    // Session operations
    int askar_session_start(long storeHandle, String profile, byte asTransaction, AskarCallback callback, long callbackId);
    int askar_session_count(long sessionHandle, String category, String tagFilter, AskarCallback callback, long callbackId);
    int askar_session_fetch(long sessionHandle, String category, String name, byte forUpdate, AskarCallback callback, long callbackId);
    int askar_session_fetch_all(long sessionHandle, String category, String tagFilter, int limit, String orderBy, byte descending, byte forUpdate, AskarCallback callback, long callbackId);
    int askar_session_update(long sessionHandle, byte operation, String category, String name, RawBuffer value, String tags, long expiryMs, AskarCallback callback, long callbackId);
    int askar_session_insert_key(long sessionHandle, long keyHandle, String name, String metadata, String tags, long expiryMs, AskarCallback callback, long callbackId);
    int askar_session_fetch_key(long sessionHandle, String name, byte forUpdate, AskarCallback callback, long callbackId);
    int askar_session_fetch_all_keys(long sessionHandle, String algorithm, String thumbprint, String tagFilter, int limit, byte forUpdate, AskarCallback callback, long callbackId);
    int askar_session_update_key(long sessionHandle, String name, String metadata, String tags, long expiryMs, AskarCallback callback, long callbackId);
    int askar_session_remove_key(long sessionHandle, String name, AskarCallback callback, long callbackId);
    int askar_session_close(long sessionHandle, byte commit, AskarCallback callback, long callbackId);

    // Key operations
    int askar_key_generate(String algorithm, String backend, byte ephemeral, PointerByReference keyHandle);
    int askar_key_from_seed(String algorithm, RawBuffer seed, String method, PointerByReference keyHandle);
    int askar_key_from_secret_bytes(String algorithm, RawBuffer secretBytes, PointerByReference keyHandle);
    int askar_key_from_public_bytes(String algorithm, RawBuffer publicBytes, PointerByReference keyHandle);
    int askar_key_from_jwk(RawBuffer jwkData, PointerByReference keyHandle);
    int askar_key_get_algorithm(long keyHandle, PointerByReference algorithm);
    int askar_key_get_ephemeral(long keyHandle, PointerByReference ephemeral);
    int askar_key_get_public_bytes(long keyHandle, PointerByReference publicBytes);
    int askar_key_get_secret_bytes(long keyHandle, PointerByReference secretBytes);
    int askar_key_get_jwk_public(long keyHandle, String algorithm, PointerByReference jwk);
    int askar_key_get_jwk_secret(long keyHandle, PointerByReference jwk);
    int askar_key_get_jwk_thumbprint(long keyHandle, String algorithm, PointerByReference thumbprint);
    int askar_key_convert(long keyHandle, String algorithm, PointerByReference convertedKeyHandle);
    int askar_key_aead_get_params(long keyHandle, PointerByReference params);
    int askar_key_aead_random_nonce(long keyHandle, PointerByReference nonce);
    int askar_key_aead_encrypt(long keyHandle, RawBuffer message, RawBuffer nonce, RawBuffer aad, PointerByReference encrypted);
    int askar_key_aead_decrypt(long keyHandle, RawBuffer ciphertext, RawBuffer nonce, RawBuffer tag, RawBuffer aad, PointerByReference decrypted);
    int askar_key_sign_message(long keyHandle, RawBuffer message, String sigType, PointerByReference signature);
    int askar_key_verify_signature(long keyHandle, RawBuffer message, RawBuffer signature, String sigType, PointerByReference valid);
    int askar_key_wrap_key(long keyHandle, long otherKeyHandle, RawBuffer nonce, PointerByReference encrypted);
    int askar_key_unwrap_key(long keyHandle, String algorithm, RawBuffer ciphertext, RawBuffer nonce, RawBuffer tag, PointerByReference unwrappedKeyHandle);
    int askar_key_exchange(String algorithm, long skHandle, long pkHandle, PointerByReference sharedKeyHandle);
    void askar_key_free(long keyHandle);

    // Entry list operations
    int askar_entry_list_count(long entryListHandle);
    int askar_entry_list_get_category(long entryListHandle, int index, PointerByReference category);
    int askar_entry_list_get_name(long entryListHandle, int index, PointerByReference name);
    int askar_entry_list_get_value(long entryListHandle, int index, RawBuffer value);
    int askar_entry_list_get_tags(long entryListHandle, int index, PointerByReference tags);
    void askar_entry_list_free(long entryListHandle);

    // Key entry list operations
    int askar_key_entry_list_count(long keyEntryListHandle);
    int askar_key_entry_list_get_algorithm(long keyEntryListHandle, int index, PointerByReference algorithm);
    int askar_key_entry_list_get_name(long keyEntryListHandle, int index, PointerByReference name);
    int askar_key_entry_list_get_metadata(long keyEntryListHandle, int index, PointerByReference metadata);
    int askar_key_entry_list_get_tags(long keyEntryListHandle, int index, PointerByReference tags);
    int askar_key_entry_list_load_key(long keyEntryListHandle, int index, PointerByReference keyHandle);
    void askar_key_entry_list_free(long keyEntryListHandle);

    // String list operations
    int askar_string_list_count(long stringListHandle);
    int askar_string_list_get_item(long stringListHandle, int index, PointerByReference item);
    void askar_string_list_free(long stringListHandle);

    // Raw key generation
    int askar_store_generate_raw_key(RawBuffer seed, PointerByReference rawKey);

    /**
     * Structure representing an input buffer to be sent to native library.
     * This is for Java->Rust data transfer and should NOT be freed with askar_buffer_free.
     */
    class InputBuffer extends Structure {
        public long len;
        public Pointer data;

        public InputBuffer() {
            super();
            this.len = 0;
            this.data = Pointer.NULL;
        }

        // Constructor for reading from existing memory location
        public InputBuffer(Pointer p) {
            super(p);
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("len", "data");
        }

        public InputBuffer(byte[] data) {
            super();
            if (data != null && data.length > 0) {
                this.len = data.length;
                this.data = new Memory(data.length);
                this.data.write(0, data, 0, data.length);
            } else {
                this.len = 0;
                this.data = Pointer.NULL;
            }
        }

        public InputBuffer(String str) {
            this(str != null ? str.getBytes() : null);
        }

        public void freeJavaMemory() {
            if (data != null && data != Pointer.NULL) {
                // JNA Memory will be garbage collected, no explicit free needed
                data = Pointer.NULL;
                len = 0;
            }
        }
        
        public byte[] toByteArray() {
            if (data == null || len == 0) {
                return new byte[0];
            }
            return data.getByteArray(0, (int) len);
        }
    }

    /**
     * Structure representing an output buffer returned from native library.
     * This represents Rust-allocated memory that MUST be freed with askar_buffer_free.
     */
    class OutputBuffer extends Structure {
        public long len;
        public Pointer data;

        public OutputBuffer() {
            super();
        }

        public OutputBuffer(Pointer p) {
            super(p);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("len", "data");
        }

        public byte[] toByteArray() {
            if (data == null || data == Pointer.NULL || len <= 0 || len > Integer.MAX_VALUE) {
                return new byte[0];
            }
            return data.getByteArray(0, (int) len);
        }

        public boolean isValid() {
            return data != null && data != Pointer.NULL && len >= 0 && len <= Integer.MAX_VALUE;
        }
    }

    /**
     * Legacy RawBuffer for backward compatibility - use InputBuffer for new code.
     * @deprecated Use InputBuffer for input data or OutputBuffer for output data
     */
    @Deprecated
    class RawBuffer extends InputBuffer {
        public RawBuffer() {
            super();
        }

        public RawBuffer(byte[] data) {
            super(data);
        }

        public RawBuffer(String str) {
            super(str);
        }

        // Constructor for reading from existing memory (Rust-allocated structures)
        public RawBuffer(Pointer p) {
            super(p);
            read();
        }

        public byte[] toByteArray() {
            if (data == null || len == 0) {
                return new byte[0];
            }
            return data.getByteArray(0, (int) len);
        }
    }

    /**
     * Structure representing AEAD parameters.
     */
    class AeadParams extends Structure {
        public int nonce_length;
        public int tag_length;

        @Override
        protected java.util.List<String> getFieldOrder() {
            return java.util.Arrays.asList("nonce_length", "tag_length");
        }
    }

    /**
     * Structure representing encrypted data.
     */
    class EncryptedData extends Structure {
        public RawBuffer buffer;
        public long tag_pos;
        public long nonce_pos;

        @Override
        protected java.util.List<String> getFieldOrder() {
            return java.util.Arrays.asList("buffer", "tag_pos", "nonce_pos");
        }
    }
}