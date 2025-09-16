#include <jni.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include "askar_ffi.h"

// Global variables for callback handling
static pthread_mutex_t callback_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t callback_cond = PTHREAD_COND_INITIALIZER;
static int64_t callback_result = 0;
static ErrorCode callback_error = 0;
static int callback_completed = 0;

// Callback for store operations (store_provision, store_open)
void store_handle_callback(int64_t callback_id, ErrorCode error_code, StoreHandle store_handle) {
    pthread_mutex_lock(&callback_mutex);
    callback_result = (int64_t)store_handle;
    callback_error = error_code;
    callback_completed = 1;
    pthread_cond_signal(&callback_cond);
    pthread_mutex_unlock(&callback_mutex);
}

// Callback for session_start
void session_handle_callback(int64_t callback_id, ErrorCode error_code, SessionHandle session_handle) {
    pthread_mutex_lock(&callback_mutex);
    callback_result = (int64_t)session_handle;
    callback_error = error_code;
    callback_completed = 1;
    pthread_cond_signal(&callback_cond);
    pthread_mutex_unlock(&callback_mutex);
}

// Callback for session_count
void count_callback(int64_t callback_id, ErrorCode error_code, int64_t count) {
    pthread_mutex_lock(&callback_mutex);
    callback_result = count;
    callback_error = error_code;
    callback_completed = 1;
    pthread_cond_signal(&callback_cond);
    pthread_mutex_unlock(&callback_mutex);
}

// Callback for entry list operations (session_fetch, session_fetch_all)
void entry_list_callback(int64_t callback_id, ErrorCode error_code, EntryListHandle entry_list_handle) {
    pthread_mutex_lock(&callback_mutex);
    // Store the EntryListHandle as a pointer value
    callback_result = (int64_t)(uintptr_t)entry_list_handle._0;
    callback_error = error_code;
    callback_completed = 1;
    pthread_cond_signal(&callback_cond);
    pthread_mutex_unlock(&callback_mutex);
}

// Callback for key operations (key_generate, key_from_seed)
void key_handle_callback(int64_t callback_id, ErrorCode error_code, LocalKeyHandle key_handle) {
    pthread_mutex_lock(&callback_mutex);
    // Store the LocalKeyHandle as a pointer value
    callback_result = (int64_t)(uintptr_t)key_handle._0;
    callback_error = error_code;
    callback_completed = 1;
    pthread_cond_signal(&callback_cond);
    pthread_mutex_unlock(&callback_mutex);
}

// Callback for functions with no return value (session_update, session_close, store_close)
void void_callback(int64_t callback_id, ErrorCode error_code) {
    pthread_mutex_lock(&callback_mutex);
    callback_result = 0; // No meaningful result
    callback_error = error_code;
    callback_completed = 1;
    pthread_cond_signal(&callback_cond);
    pthread_mutex_unlock(&callback_mutex);
}

// Wait for callback completion
ErrorCode wait_for_callback() {
    pthread_mutex_lock(&callback_mutex);
    while (!callback_completed) {
        pthread_cond_wait(&callback_cond, &callback_mutex);
    }
    ErrorCode result = callback_error;
    callback_completed = 0; // Reset for next call
    pthread_mutex_unlock(&callback_mutex);
    return result;
}

// Helper function to convert Java string to C string
const char* get_string_utf(JNIEnv *env, jstring jstr) {
    if (jstr == NULL) return NULL;
    return (*env)->GetStringUTFChars(env, jstr, NULL);
}

// Helper function to release Java string
void release_string_utf(JNIEnv *env, jstring jstr, const char* cstr) {
    if (jstr != NULL && cstr != NULL) {
        (*env)->ReleaseStringUTFChars(env, jstr, cstr);
    }
}

// Basic test functions
JNIEXPORT jstring JNICALL Java_org_hyperledger_aries_askar_AskarNative_getVersion
  (JNIEnv *env, jclass cls) {
    char* version = askar_version();
    if (version == NULL) return NULL;
    
    jstring result = (*env)->NewStringUTF(env, version);
    return result;
}

JNIEXPORT jint JNICALL Java_org_hyperledger_aries_askar_AskarNative_setMaxLogLevel
  (JNIEnv *env, jclass cls, jint level) {
    return (jint)askar_set_max_log_level((int32_t)level);
}

JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_storeProvision
  (JNIEnv *env, jclass cls, jstring uri, jstring keyMethod, jstring passKey, jstring profile, jboolean recreate) {
    
    const char* c_uri = get_string_utf(env, uri);
    const char* c_key_method = get_string_utf(env, keyMethod);
    const char* c_pass_key = get_string_utf(env, passKey);
    const char* c_profile = get_string_utf(env, profile);
    
    ErrorCode result = askar_store_provision(
        c_uri, c_key_method, c_pass_key, c_profile, 
        (int8_t)recreate, store_handle_callback, 1
    );
    
    release_string_utf(env, uri, c_uri);
    release_string_utf(env, keyMethod, c_key_method);
    release_string_utf(env, passKey, c_pass_key);
    release_string_utf(env, profile, c_profile);
    
    if (result != 0) {
        return 0;
    }
    
    ErrorCode callback_err = wait_for_callback();
    if (callback_err != 0) {
        return 0;
    }
    
    return (jlong)callback_result;
}

JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_sessionStart
  (JNIEnv *env, jclass cls, jlong storeHandle, jstring profile, jboolean asTransaction) {
    
    const char* c_profile = get_string_utf(env, profile);
    
    ErrorCode result = askar_session_start(
        (StoreHandle)storeHandle, c_profile, 
        (int8_t)asTransaction, session_handle_callback, 4
    );
    
    release_string_utf(env, profile, c_profile);
    
    if (result != 0) {
        return 0;
    }
    
    ErrorCode callback_err = wait_for_callback();
    if (callback_err != 0) {
        return 0;
    }
    
    return (jlong)callback_result;
}

JNIEXPORT void JNICALL Java_org_hyperledger_aries_askar_AskarNative_sessionUpdate
  (JNIEnv *env, jclass cls, jlong sessionHandle, jbyte operation, jstring category, 
   jstring name, jbyteArray value, jstring tags, jlong expiryMs) {
    
    const char* c_category = get_string_utf(env, category);
    const char* c_name = get_string_utf(env, name);
    const char* c_tags = get_string_utf(env, tags);
    
    // Convert byte array to ByteBuffer
    struct ByteBuffer buffer = {0};
    jbyte* value_data = NULL;
    if (value != NULL) {
        jsize len = (*env)->GetArrayLength(env, value);
        buffer.len = (int64_t)len;
        if (len > 0) {
            value_data = (*env)->GetByteArrayElements(env, value, NULL);
            buffer.data = (uint8_t*)value_data;
        }
    }
    
    printf("DEBUG: sessionUpdate called with handle=%ld (0x%lx), operation=%d, category=%s, name=%s, value_len=%ld, tags=%s, expiry=%ld\n",
            sessionHandle, sessionHandle, operation, c_category ? c_category : "NULL", c_name ? c_name : "NULL", 
            buffer.len, c_tags ? c_tags : "NULL", expiryMs);
    fflush(stdout);
    
    ErrorCode result = askar_session_update(
        (SessionHandle)sessionHandle, (int8_t)operation, c_category, c_name,
        buffer, c_tags, expiryMs >= 0 ? expiryMs : -1,
        void_callback, 8
    );
    
    if (result != 0) {
        // Get detailed error message from Rust
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: askar_session_update returned error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Direct error details: %s\n", error_json);
        }
        
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[512];
            if (error_json) {
                snprintf(error_msg, sizeof(error_msg), "Session update failed with error code: %ld, detail: %s", result, error_json);
            } else {
                snprintf(error_msg, sizeof(error_msg), "Session update failed with error code: %ld", result);
            }
            (*env)->ThrowNew(env, exc, error_msg);
        }
        goto cleanup;
    }
    
    ErrorCode callback_err = wait_for_callback();
    if (callback_err != 0) {
        // Get detailed error message from Rust
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: sessionUpdate callback failed with error code: %ld\n", callback_err);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[512];
            if (error_json) {
                snprintf(error_msg, sizeof(error_msg), "Session update callback failed with error code: %ld, detail: %s", callback_err, error_json);
            } else {
                snprintf(error_msg, sizeof(error_msg), "Session update callback failed with error code: %ld", callback_err);
            }
            (*env)->ThrowNew(env, exc, error_msg);
        }
    } else {
        printf("DEBUG: sessionUpdate succeeded!\n");
    }
    
cleanup:
    release_string_utf(env, category, c_category);
    release_string_utf(env, name, c_name);
    release_string_utf(env, tags, c_tags);
    if (value_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, value, value_data, JNI_ABORT);
    }
}

JNIEXPORT void JNICALL Java_org_hyperledger_aries_askar_AskarNative_sessionClose
  (JNIEnv *env, jclass cls, jlong sessionHandle, jboolean commit) {
    
    ErrorCode result = askar_session_close(
        (SessionHandle)sessionHandle, (int8_t)commit, void_callback, 7
    );
    
    if (result != 0) {
        printf("DEBUG: askar_session_close returned error: %ld\n", result);
        return;
    }
    
    ErrorCode callback_err = wait_for_callback();
    if (callback_err != 0) {
        printf("DEBUG: sessionClose callback failed with error code: %ld\n", callback_err);
    }
}

JNIEXPORT void JNICALL Java_org_hyperledger_aries_askar_AskarNative_storeClose
  (JNIEnv *env, jclass cls, jlong storeHandle) {
    
    ErrorCode result = askar_store_close((StoreHandle)storeHandle, void_callback, 3);
    
    if (result != 0) {
        printf("DEBUG: askar_store_close returned error: %ld\n", result);
        return;
    }
    
    ErrorCode callback_err = wait_for_callback();
    if (callback_err != 0) {
        printf("DEBUG: storeClose callback failed with error code: %ld\n", callback_err);
    }
}

JNIEXPORT jint JNICALL Java_org_hyperledger_aries_askar_AskarNative_sessionCount
  (JNIEnv *env, jclass cls, jlong sessionHandle, jstring category, jstring tagFilter) {
    
    const char* c_category = get_string_utf(env, category);
    const char* c_tag_filter = get_string_utf(env, tagFilter);
    
    printf("DEBUG: sessionCount called with handle=%ld, category=%s, tagFilter=%s\n",
            sessionHandle, c_category ? c_category : "NULL", c_tag_filter ? c_tag_filter : "NULL");
    fflush(stdout);
    
    ErrorCode result = askar_session_count(
        (SessionHandle)sessionHandle, c_category, c_tag_filter,
        count_callback, 5
    );
    
    release_string_utf(env, category, c_category);
    release_string_utf(env, tagFilter, c_tag_filter);
    
    if (result != 0) {
        printf("DEBUG: askar_session_count returned error: %ld\n", result);
        return -1;
    }
    
    ErrorCode callback_err = wait_for_callback();
    if (callback_err != 0) {
        printf("DEBUG: sessionCount callback failed with error code: %ld\n", callback_err);
        return -1;
    }
    
    printf("DEBUG: sessionCount succeeded, count=%ld\n", callback_result);
    return (jint)callback_result;
}

JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_sessionFetch
  (JNIEnv *env, jclass cls, jlong sessionHandle, jstring category, jstring name, jboolean forUpdate) {
    
    const char* c_category = get_string_utf(env, category);
    const char* c_name = get_string_utf(env, name);
    
    printf("DEBUG: sessionFetch called with handle=%ld, category=%s, name=%s, forUpdate=%d\n",
            sessionHandle, c_category ? c_category : "NULL", c_name ? c_name : "NULL", forUpdate);
    fflush(stdout);
    
    ErrorCode result = askar_session_fetch(
        (SessionHandle)sessionHandle, c_category, c_name, (int8_t)forUpdate,
        entry_list_callback, 9
    );
    
    release_string_utf(env, category, c_category);
    release_string_utf(env, name, c_name);
    
    if (result != 0) {
        printf("DEBUG: askar_session_fetch returned error: %ld\n", result);
        return 0;
    }
    
    ErrorCode callback_err = wait_for_callback();
    if (callback_err != 0) {
        printf("DEBUG: sessionFetch callback failed with error code: %ld\n", callback_err);
        return 0;
    }
    
    printf("DEBUG: sessionFetch succeeded, entryListHandle=%ld\n", callback_result);
    return (jlong)callback_result;
}

JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_sessionFetchAll
  (JNIEnv *env, jclass cls, jlong sessionHandle, jstring category, jstring tagFilter, 
   jint limit, jstring orderBy, jboolean descending, jboolean forUpdate) {
    
    const char* c_category = get_string_utf(env, category);
    const char* c_tag_filter = get_string_utf(env, tagFilter);
    const char* c_order_by = get_string_utf(env, orderBy);
    
    printf("DEBUG: sessionFetchAll called with handle=%ld, category=%s, tagFilter=%s, limit=%d, orderBy=%s, descending=%d, forUpdate=%d\n",
            sessionHandle, c_category ? c_category : "NULL", c_tag_filter ? c_tag_filter : "NULL", 
            limit, c_order_by ? c_order_by : "NULL", descending, forUpdate);
    fflush(stdout);
    
    ErrorCode result = askar_session_fetch_all(
        (SessionHandle)sessionHandle, c_category, c_tag_filter,
        (int64_t)limit, c_order_by, (int8_t)descending, (int8_t)forUpdate,
        entry_list_callback, 6
    );
    
    release_string_utf(env, category, c_category);
    release_string_utf(env, tagFilter, c_tag_filter);
    release_string_utf(env, orderBy, c_order_by);
    
    if (result != 0) {
        printf("DEBUG: askar_session_fetch_all returned error: %ld\n", result);
        return 0;
    }
    
    ErrorCode callback_err = wait_for_callback();
    if (callback_err != 0) {
        printf("DEBUG: sessionFetchAll callback failed with error code: %ld\n", callback_err);
        return 0;
    }
    
    printf("DEBUG: sessionFetchAll succeeded, entryListHandle=%ld\n", callback_result);
    return (jlong)callback_result;
}

// Entry list operations - basic implementations for testing
JNIEXPORT jint JNICALL Java_org_hyperledger_aries_askar_AskarNative_entryListCount
  (JNIEnv *env, jclass cls, jlong entryListHandle) {
    
    if (entryListHandle == 0) {
        return 0;
    }
    
    // Reconstruct EntryListHandle from pointer
    EntryListHandle handle = {(const FfiEntryList*)entryListHandle};
    int32_t count = 0;
    
    ErrorCode result = askar_entry_list_count(handle, &count);
    if (result != 0) {
        printf("DEBUG: askar_entry_list_count returned error: %ld\n", result);
        return 0;
    }
    
    printf("DEBUG: entryListCount succeeded, count=%d\n", count);
    return (jint)count;
}

JNIEXPORT jstring JNICALL Java_org_hyperledger_aries_askar_AskarNative_entryListGetCategory
  (JNIEnv *env, jclass cls, jlong entryListHandle, jint index) {
    
    if (entryListHandle == 0) {
        return NULL;
    }
    
    EntryListHandle handle = {(const FfiEntryList*)entryListHandle};
    const char* category = NULL;
    
    ErrorCode result = askar_entry_list_get_category(handle, (int32_t)index, &category);
    if (result != 0 || category == NULL) {
        printf("DEBUG: askar_entry_list_get_category returned error: %ld\n", result);
        return NULL;
    }
    
    return (*env)->NewStringUTF(env, category);
}

JNIEXPORT jstring JNICALL Java_org_hyperledger_aries_askar_AskarNative_entryListGetName
  (JNIEnv *env, jclass cls, jlong entryListHandle, jint index) {
    
    if (entryListHandle == 0) {
        return NULL;
    }
    
    EntryListHandle handle = {(const FfiEntryList*)entryListHandle};
    const char* name = NULL;
    
    ErrorCode result = askar_entry_list_get_name(handle, (int32_t)index, &name);
    if (result != 0 || name == NULL) {
        printf("DEBUG: askar_entry_list_get_name returned error: %ld\n", result);
        return NULL;
    }
    
    return (*env)->NewStringUTF(env, name);
}

JNIEXPORT jbyteArray JNICALL Java_org_hyperledger_aries_askar_AskarNative_entryListGetValue
  (JNIEnv *env, jclass cls, jlong entryListHandle, jint index) {
    
    if (entryListHandle == 0) {
        return NULL;
    }
    
    EntryListHandle handle = {(const FfiEntryList*)entryListHandle};
    struct SecretBuffer buffer = {0};
    
    ErrorCode result = askar_entry_list_get_value(handle, (int32_t)index, &buffer);
    if (result != 0 || buffer.data == NULL) {
        printf("DEBUG: askar_entry_list_get_value returned error: %ld\n", result);
        return NULL;
    }
    
    jbyteArray jvalue = (*env)->NewByteArray(env, (jsize)buffer.len);
    if (jvalue == NULL) {
        return NULL;
    }
    
    (*env)->SetByteArrayRegion(env, jvalue, 0, (jsize)buffer.len, (jbyte*)buffer.data);
    return jvalue;
}

JNIEXPORT jstring JNICALL Java_org_hyperledger_aries_askar_AskarNative_entryListGetTags
  (JNIEnv *env, jclass cls, jlong entryListHandle, jint index) {
    
    if (entryListHandle == 0) {
        return NULL;
    }
    
    EntryListHandle handle = {(const FfiEntryList*)entryListHandle};
    const char* tags = NULL;
    
    ErrorCode result = askar_entry_list_get_tags(handle, (int32_t)index, &tags);
    if (result != 0 || tags == NULL) {
        printf("DEBUG: askar_entry_list_get_tags returned error: %ld\n", result);
        return NULL;
    }
    
    return (*env)->NewStringUTF(env, tags);
}

JNIEXPORT void JNICALL Java_org_hyperledger_aries_askar_AskarNative_entryListFree
  (JNIEnv *env, jclass cls, jlong entryListHandle) {
    
    if (entryListHandle == 0) {
        return;
    }
    
    EntryListHandle handle = {(const FfiEntryList*)entryListHandle};
    askar_entry_list_free(handle);
    
    printf("DEBUG: entryListFree completed for handle=%ld\n", entryListHandle);
}

// Key management functions
JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyGenerate
  (JNIEnv *env, jclass cls, jstring algorithm, jstring keyBackend, jboolean ephemeral) {
    
    const char* c_algorithm = get_string_utf(env, algorithm);
    const char* c_key_backend = get_string_utf(env, keyBackend);
    
    printf("DEBUG: keyGenerate called with algorithm=%s, keyBackend=%s, ephemeral=%d\n",
            c_algorithm ? c_algorithm : "NULL", c_key_backend ? c_key_backend : "NULL", ephemeral);
    fflush(stdout);
    
    LocalKeyHandle key_handle = {NULL};
    ErrorCode result = askar_key_generate(
        c_algorithm, c_key_backend, (int8_t)ephemeral, &key_handle
    );
    
    release_string_utf(env, algorithm, c_algorithm);
    release_string_utf(env, keyBackend, c_key_backend);
    
    if (result != 0) {
        // Get detailed error message from Rust
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: askar_key_generate returned error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[512];
            if (error_json) {
                snprintf(error_msg, sizeof(error_msg), "Key generation failed with error code: %ld, detail: %s", result, error_json);
            } else {
                snprintf(error_msg, sizeof(error_msg), "Key generation failed with error code: %ld", result);
            }
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return 0;
    }
    
    // Store the LocalKeyHandle as a pointer value
    jlong handle_value = (jlong)(uintptr_t)key_handle._0;
    printf("DEBUG: keyGenerate succeeded, keyHandle=%ld\n", handle_value);
    return handle_value;
}

JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyFromSeed
  (JNIEnv *env, jclass cls, jstring algorithm, jbyteArray seed, jstring method) {
    
    const char* c_algorithm = get_string_utf(env, algorithm);
    const char* c_method = get_string_utf(env, method);
    
    // Convert byte array to ByteBuffer
    struct ByteBuffer buffer = {0};
    jbyte* seed_data = NULL;
    if (seed != NULL) {
        jsize len = (*env)->GetArrayLength(env, seed);
        buffer.len = (int64_t)len;
        if (len > 0) {
            seed_data = (*env)->GetByteArrayElements(env, seed, NULL);
            buffer.data = (uint8_t*)seed_data;
        }
    }
    
    printf("DEBUG: keyFromSeed called with algorithm=%s, seed_len=%ld, method=%s\n",
            c_algorithm ? c_algorithm : "NULL", buffer.len, c_method ? c_method : "NULL");
    fflush(stdout);
    
    LocalKeyHandle key_handle = {NULL};
    ErrorCode result = askar_key_from_seed(
        c_algorithm, buffer, c_method, &key_handle
    );
    
    release_string_utf(env, algorithm, c_algorithm);
    release_string_utf(env, method, c_method);
    if (seed_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, seed, seed_data, JNI_ABORT);
    }
    
    if (result != 0) {
        // Get detailed error message from Rust
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: askar_key_from_seed returned error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[512];
            if (error_json) {
                snprintf(error_msg, sizeof(error_msg), "Key from seed failed with error code: %ld, detail: %s", result, error_json);
            } else {
                snprintf(error_msg, sizeof(error_msg), "Key from seed failed with error code: %ld", result);
            }
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return 0;
    }
    
    // Store the LocalKeyHandle as a pointer value
    jlong handle_value = (jlong)(uintptr_t)key_handle._0;
    printf("DEBUG: keyFromSeed succeeded, keyHandle=%ld\n", handle_value);
    return handle_value;
}

JNIEXPORT void JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyFree
  (JNIEnv *env, jclass cls, jlong keyHandle) {
    
    if (keyHandle == 0) {
        return;
    }
    
    printf("DEBUG: keyFree called with handle=%ld\n", keyHandle);
    fflush(stdout);
    
    // Reconstruct LocalKeyHandle from pointer
    LocalKeyHandle handle = {(const struct LocalKey*)keyHandle};
    askar_key_free(handle);
    
    printf("DEBUG: keyFree completed for handle=%ld\n", keyHandle);
}

// Advanced key operations
JNIEXPORT jstring JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyGetAlgorithm
  (JNIEnv *env, jclass cls, jlong keyHandle) {
    
    if (keyHandle == 0) {
        return NULL;
    }
    
    printf("DEBUG: keyGetAlgorithm called with handle=%ld\n", keyHandle);
    fflush(stdout);
    
    LocalKeyHandle handle = {(const struct LocalKey*)keyHandle};
    const char* algorithm = NULL;
    
    ErrorCode result = askar_key_get_algorithm(handle, &algorithm);
    if (result != 0) {
        printf("DEBUG: askar_key_get_algorithm returned error: %ld\n", result);
        return NULL;
    }
    
    if (algorithm == NULL) {
        printf("DEBUG: algorithm is NULL\n");
        return NULL;
    }
    
    printf("DEBUG: keyGetAlgorithm succeeded, algorithm=%s\n", algorithm);
    return (*env)->NewStringUTF(env, algorithm);
}

JNIEXPORT jbyteArray JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyGetPublicBytes
  (JNIEnv *env, jclass cls, jlong keyHandle) {
    
    if (keyHandle == 0) {
        return NULL;
    }
    
    printf("DEBUG: keyGetPublicBytes called with handle=%ld\n", keyHandle);
    fflush(stdout);
    
    LocalKeyHandle handle = {(const struct LocalKey*)keyHandle};
    struct SecretBuffer buffer = {0};
    
    ErrorCode result = askar_key_get_public_bytes(handle, &buffer);
    if (result != 0) {
        printf("DEBUG: askar_key_get_public_bytes returned error: %ld\n", result);
        return NULL;
    }
    
    if (buffer.data == NULL || buffer.len <= 0) {
        printf("DEBUG: public bytes buffer is empty\n");
        return NULL;
    }
    
    jbyteArray jbytes = (*env)->NewByteArray(env, (jsize)buffer.len);
    if (jbytes == NULL) {
        return NULL;
    }
    
    (*env)->SetByteArrayRegion(env, jbytes, 0, (jsize)buffer.len, (jbyte*)buffer.data);
    printf("DEBUG: keyGetPublicBytes succeeded, length=%ld\n", buffer.len);
    return jbytes;
}

JNIEXPORT jbyteArray JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyGetSecretBytes
  (JNIEnv *env, jclass cls, jlong keyHandle) {
    
    if (keyHandle == 0) {
        return NULL;
    }
    
    printf("DEBUG: keyGetSecretBytes called with handle=%ld\n", keyHandle);
    fflush(stdout);
    
    LocalKeyHandle handle = {(const struct LocalKey*)keyHandle};
    struct SecretBuffer buffer = {0};
    
    ErrorCode result = askar_key_get_secret_bytes(handle, &buffer);
    if (result != 0) {
        printf("DEBUG: askar_key_get_secret_bytes returned error: %ld\n", result);
        return NULL;
    }
    
    if (buffer.data == NULL || buffer.len <= 0) {
        printf("DEBUG: secret bytes buffer is empty\n");
        return NULL;
    }
    
    jbyteArray jbytes = (*env)->NewByteArray(env, (jsize)buffer.len);
    if (jbytes == NULL) {
        return NULL;
    }
    
    (*env)->SetByteArrayRegion(env, jbytes, 0, (jsize)buffer.len, (jbyte*)buffer.data);
    printf("DEBUG: keyGetSecretBytes succeeded, length=%ld\n", buffer.len);
    return jbytes;
}

JNIEXPORT jstring JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyGetJwkPublic
  (JNIEnv *env, jclass cls, jlong keyHandle, jstring algorithm) {
    
    if (keyHandle == 0) {
        return NULL;
    }
    
    const char* c_algorithm = get_string_utf(env, algorithm);
    
    printf("DEBUG: keyGetJwkPublic called with handle=%ld, algorithm=%s\n", 
            keyHandle, c_algorithm ? c_algorithm : "NULL");
    fflush(stdout);
    
    LocalKeyHandle handle = {(const struct LocalKey*)keyHandle};
    const char* jwk_public = NULL;
    
    ErrorCode result = askar_key_get_jwk_public(handle, c_algorithm, &jwk_public);
    
    release_string_utf(env, algorithm, c_algorithm);
    
    if (result != 0) {
        printf("DEBUG: askar_key_get_jwk_public returned error: %ld\n", result);
        return NULL;
    }
    
    if (jwk_public == NULL) {
        printf("DEBUG: JWK public is NULL\n");
        return NULL;
    }
    
    printf("DEBUG: keyGetJwkPublic succeeded\n");
    return (*env)->NewStringUTF(env, jwk_public);
}

JNIEXPORT jbyteArray JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyGetJwkSecret
  (JNIEnv *env, jclass cls, jlong keyHandle) {
    
    if (keyHandle == 0) {
        return NULL;
    }
    
    printf("DEBUG: keyGetJwkSecret called with handle=%ld\n", keyHandle);
    fflush(stdout);
    
    LocalKeyHandle handle = {(const struct LocalKey*)keyHandle};
    struct SecretBuffer buffer = {0};
    
    ErrorCode result = askar_key_get_jwk_secret(handle, &buffer);
    if (result != 0) {
        printf("DEBUG: askar_key_get_jwk_secret returned error: %ld\n", result);
        return NULL;
    }
    
    if (buffer.data == NULL || buffer.len <= 0) {
        printf("DEBUG: JWK secret buffer is empty\n");
        return NULL;
    }
    
    jbyteArray jbytes = (*env)->NewByteArray(env, (jsize)buffer.len);
    if (jbytes == NULL) {
        return NULL;
    }
    
    (*env)->SetByteArrayRegion(env, jbytes, 0, (jsize)buffer.len, (jbyte*)buffer.data);
    printf("DEBUG: keyGetJwkSecret succeeded, length=%ld\n", buffer.len);
    return jbytes;
}

JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyFromJwk
  (JNIEnv *env, jclass cls, jbyteArray jwkData) {
    
    if (jwkData == NULL) {
        return 0;
    }
    
    // Convert byte array to ByteBuffer
    struct ByteBuffer buffer = {0};
    jbyte* jwk_data = NULL;
    jsize len = (*env)->GetArrayLength(env, jwkData);
    buffer.len = (int64_t)len;
    if (len > 0) {
        jwk_data = (*env)->GetByteArrayElements(env, jwkData, NULL);
        buffer.data = (uint8_t*)jwk_data;
    }
    
    printf("DEBUG: keyFromJwk called with jwk_len=%ld\n", buffer.len);
    fflush(stdout);
    
    LocalKeyHandle key_handle = {NULL};
    ErrorCode result = askar_key_from_jwk(buffer, &key_handle);
    
    if (jwk_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, jwkData, jwk_data, JNI_ABORT);
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: askar_key_from_jwk returned error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[512];
            if (error_json) {
                snprintf(error_msg, sizeof(error_msg), "Key from JWK failed with error code: %ld, detail: %s", result, error_json);
            } else {
                snprintf(error_msg, sizeof(error_msg), "Key from JWK failed with error code: %ld", result);
            }
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return 0;
    }
    
    jlong handle_value = (jlong)(uintptr_t)key_handle._0;
    printf("DEBUG: keyFromJwk succeeded, keyHandle=%ld\n", handle_value);
    return handle_value;
}

JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyFromPublicBytes
  (JNIEnv *env, jclass cls, jstring algorithm, jbyteArray publicBytes) {
    
    if (publicBytes == NULL) {
        return 0;
    }
    
    const char* c_algorithm = get_string_utf(env, algorithm);
    
    // Convert byte array to ByteBuffer
    struct ByteBuffer buffer = {0};
    jbyte* public_data = NULL;
    jsize len = (*env)->GetArrayLength(env, publicBytes);
    buffer.len = (int64_t)len;
    if (len > 0) {
        public_data = (*env)->GetByteArrayElements(env, publicBytes, NULL);
        buffer.data = (uint8_t*)public_data;
    }
    
    printf("DEBUG: keyFromPublicBytes called with algorithm=%s, public_len=%ld\n",
            c_algorithm ? c_algorithm : "NULL", buffer.len);
    fflush(stdout);
    
    LocalKeyHandle key_handle = {NULL};
    ErrorCode result = askar_key_from_public_bytes(c_algorithm, buffer, &key_handle);
    
    release_string_utf(env, algorithm, c_algorithm);
    if (public_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, publicBytes, public_data, JNI_ABORT);
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: askar_key_from_public_bytes returned error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[512];
            if (error_json) {
                snprintf(error_msg, sizeof(error_msg), "Key from public bytes failed with error code: %ld, detail: %s", result, error_json);
            } else {
                snprintf(error_msg, sizeof(error_msg), "Key from public bytes failed with error code: %ld", result);
            }
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return 0;
    }
    
    jlong handle_value = (jlong)(uintptr_t)key_handle._0;
    printf("DEBUG: keyFromPublicBytes succeeded, keyHandle=%ld\n", handle_value);
    return handle_value;
}

JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyFromSecretBytes
  (JNIEnv *env, jclass cls, jstring algorithm, jbyteArray secretBytes) {
    
    if (secretBytes == NULL) {
        return 0;
    }
    
    const char* c_algorithm = get_string_utf(env, algorithm);
    
    // Convert byte array to ByteBuffer
    struct ByteBuffer buffer = {0};
    jbyte* secret_data = NULL;
    jsize len = (*env)->GetArrayLength(env, secretBytes);
    buffer.len = (int64_t)len;
    if (len > 0) {
        secret_data = (*env)->GetByteArrayElements(env, secretBytes, NULL);
        buffer.data = (uint8_t*)secret_data;
    }
    
    printf("DEBUG: keyFromSecretBytes called with algorithm=%s, secret_len=%ld\n",
            c_algorithm ? c_algorithm : "NULL", buffer.len);
    fflush(stdout);
    
    LocalKeyHandle key_handle = {NULL};
    ErrorCode result = askar_key_from_secret_bytes(c_algorithm, buffer, &key_handle);
    
    release_string_utf(env, algorithm, c_algorithm);
    if (secret_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, secretBytes, secret_data, JNI_ABORT);
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: askar_key_from_secret_bytes returned error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[512];
            if (error_json) {
                snprintf(error_msg, sizeof(error_msg), "Key from secret bytes failed with error code: %ld, detail: %s", result, error_json);
            } else {
                snprintf(error_msg, sizeof(error_msg), "Key from secret bytes failed with error code: %ld", result);
            }
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return 0;
    }
    
    jlong handle_value = (jlong)(uintptr_t)key_handle._0;
    printf("DEBUG: keyFromSecretBytes succeeded, keyHandle=%ld\n", handle_value);
    return handle_value;
}

// Cryptographic operations
JNIEXPORT jbyteArray JNICALL Java_org_hyperledger_aries_askar_AskarNative_keySignMessage
  (JNIEnv *env, jclass cls, jlong keyHandle, jbyteArray message, jstring signatureType) {
    
    if (keyHandle == 0 || message == NULL) {
        return NULL;
    }
    
    const char* c_sig_type = get_string_utf(env, signatureType);
    
    // Convert message byte array to ByteBuffer
    struct ByteBuffer buffer = {0};
    jbyte* message_data = NULL;
    jsize len = (*env)->GetArrayLength(env, message);
    buffer.len = (int64_t)len;
    if (len > 0) {
        message_data = (*env)->GetByteArrayElements(env, message, NULL);
        buffer.data = (uint8_t*)message_data;
    }
    
    printf("DEBUG: keySignMessage called with handle=%ld, message_len=%ld, sig_type=%s\n",
            keyHandle, buffer.len, c_sig_type ? c_sig_type : "NULL");
    fflush(stdout);
    
    LocalKeyHandle handle = {(const struct LocalKey*)keyHandle};
    struct SecretBuffer signature_buffer = {0};
    
    ErrorCode result = askar_key_sign_message(handle, buffer, c_sig_type, &signature_buffer);
    
    release_string_utf(env, signatureType, c_sig_type);
    if (message_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, message, message_data, JNI_ABORT);
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: askar_key_sign_message returned error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[512];
            if (error_json) {
                snprintf(error_msg, sizeof(error_msg), "Message signing failed with error code: %ld, detail: %s", result, error_json);
            } else {
                snprintf(error_msg, sizeof(error_msg), "Message signing failed with error code: %ld", result);
            }
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return NULL;
    }
    
    if (signature_buffer.data == NULL || signature_buffer.len <= 0) {
        printf("DEBUG: signature buffer is empty\n");
        return NULL;
    }
    
    jbyteArray jsignature = (*env)->NewByteArray(env, (jsize)signature_buffer.len);
    if (jsignature == NULL) {
        return NULL;
    }
    
    (*env)->SetByteArrayRegion(env, jsignature, 0, (jsize)signature_buffer.len, (jbyte*)signature_buffer.data);
    printf("DEBUG: keySignMessage succeeded, signature_len=%ld\n", signature_buffer.len);
    return jsignature;
}

JNIEXPORT jboolean JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyVerifySignature
  (JNIEnv *env, jclass cls, jlong keyHandle, jbyteArray message, jbyteArray signature, jstring signatureType) {
    
    if (keyHandle == 0 || message == NULL || signature == NULL) {
        return JNI_FALSE;
    }
    
    const char* c_sig_type = get_string_utf(env, signatureType);
    
    // Convert message byte array to ByteBuffer
    struct ByteBuffer message_buffer = {0};
    jbyte* message_data = NULL;
    jsize message_len = (*env)->GetArrayLength(env, message);
    message_buffer.len = (int64_t)message_len;
    if (message_len > 0) {
        message_data = (*env)->GetByteArrayElements(env, message, NULL);
        message_buffer.data = (uint8_t*)message_data;
    }
    
    // Convert signature byte array to ByteBuffer
    struct ByteBuffer sig_buffer = {0};
    jbyte* sig_data = NULL;
    jsize sig_len = (*env)->GetArrayLength(env, signature);
    sig_buffer.len = (int64_t)sig_len;
    if (sig_len > 0) {
        sig_data = (*env)->GetByteArrayElements(env, signature, NULL);
        sig_buffer.data = (uint8_t*)sig_data;
    }
    
    printf("DEBUG: keyVerifySignature called with handle=%ld, message_len=%ld, sig_len=%ld, sig_type=%s\n",
            keyHandle, message_buffer.len, sig_buffer.len, c_sig_type ? c_sig_type : "NULL");
    fflush(stdout);
    
    LocalKeyHandle handle = {(const struct LocalKey*)keyHandle};
    int8_t verification_result = 0;
    
    ErrorCode result = askar_key_verify_signature(handle, message_buffer, sig_buffer, c_sig_type, &verification_result);
    
    release_string_utf(env, signatureType, c_sig_type);
    if (message_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, message, message_data, JNI_ABORT);
    }
    if (sig_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, signature, sig_data, JNI_ABORT);
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: askar_key_verify_signature returned error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        return JNI_FALSE;
    }
    
    printf("DEBUG: keyVerifySignature succeeded, verification_result=%d\n", verification_result);
    return verification_result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jbyteArray JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyAeadEncrypt
  (JNIEnv *env, jclass cls, jlong keyHandle, jbyteArray message, jbyteArray nonce, jbyteArray aad) {
    
    if (keyHandle == 0 || message == NULL) {
        return NULL;
    }
    
    // Convert message byte array to ByteBuffer
    struct ByteBuffer message_buffer = {0};
    jbyte* message_data = NULL;
    jsize message_len = (*env)->GetArrayLength(env, message);
    message_buffer.len = (int64_t)message_len;
    if (message_len > 0) {
        message_data = (*env)->GetByteArrayElements(env, message, NULL);
        message_buffer.data = (uint8_t*)message_data;
    }
    
    // Convert nonce byte array to ByteBuffer (optional)
    struct ByteBuffer nonce_buffer = {0};
    jbyte* nonce_data = NULL;
    if (nonce != NULL) {
        jsize nonce_len = (*env)->GetArrayLength(env, nonce);
        nonce_buffer.len = (int64_t)nonce_len;
        if (nonce_len > 0) {
            nonce_data = (*env)->GetByteArrayElements(env, nonce, NULL);
            nonce_buffer.data = (uint8_t*)nonce_data;
        }
    }
    
    // Convert aad byte array to ByteBuffer (optional)
    struct ByteBuffer aad_buffer = {0};
    jbyte* aad_data = NULL;
    if (aad != NULL) {
        jsize aad_len = (*env)->GetArrayLength(env, aad);
        aad_buffer.len = (int64_t)aad_len;
        if (aad_len > 0) {
            aad_data = (*env)->GetByteArrayElements(env, aad, NULL);
            aad_buffer.data = (uint8_t*)aad_data;
        }
    }
    
    printf("DEBUG: keyAeadEncrypt called with handle=%ld, message_len=%ld, nonce_len=%ld, aad_len=%ld\n",
            keyHandle, message_buffer.len, nonce_buffer.len, aad_buffer.len);
    fflush(stdout);
    
    LocalKeyHandle handle = {(const struct LocalKey*)keyHandle};
    struct EncryptedBuffer encrypted_buffer = {0};
    
    ErrorCode result = askar_key_aead_encrypt(handle, message_buffer, nonce_buffer, aad_buffer, &encrypted_buffer);
    
    if (message_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, message, message_data, JNI_ABORT);
    }
    if (nonce_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, nonce, nonce_data, JNI_ABORT);
    }
    if (aad_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, aad, aad_data, JNI_ABORT);
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: askar_key_aead_encrypt returned error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[512];
            if (error_json) {
                snprintf(error_msg, sizeof(error_msg), "AEAD encryption failed with error code: %ld, detail: %s", result, error_json);
            } else {
                snprintf(error_msg, sizeof(error_msg), "AEAD encryption failed with error code: %ld", result);
            }
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return NULL;
    }
    
    if (encrypted_buffer.buffer.data == NULL || encrypted_buffer.buffer.len <= 0) {
        printf("DEBUG: encrypted buffer is empty\n");
        return NULL;
    }
    
    jbyteArray jencrypted = (*env)->NewByteArray(env, (jsize)encrypted_buffer.buffer.len);
    if (jencrypted == NULL) {
        return NULL;
    }
    
    (*env)->SetByteArrayRegion(env, jencrypted, 0, (jsize)encrypted_buffer.buffer.len, (jbyte*)encrypted_buffer.buffer.data);
    printf("DEBUG: keyAeadEncrypt succeeded, encrypted_len=%ld, tag_pos=%ld, nonce_pos=%ld\n", 
           encrypted_buffer.buffer.len, encrypted_buffer.tag_pos, encrypted_buffer.nonce_pos);
    return jencrypted;
}

JNIEXPORT jbyteArray JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyAeadDecrypt
  (JNIEnv *env, jclass cls, jlong keyHandle, jbyteArray ciphertext, jbyteArray nonce, jbyteArray tag, jbyteArray aad) {
    
    if (keyHandle == 0 || ciphertext == NULL) {
        return NULL;
    }
    
    // Convert ciphertext byte array to ByteBuffer
    struct ByteBuffer ciphertext_buffer = {0};
    jbyte* ciphertext_data = NULL;
    jsize ciphertext_len = (*env)->GetArrayLength(env, ciphertext);
    ciphertext_buffer.len = (int64_t)ciphertext_len;
    if (ciphertext_len > 0) {
        ciphertext_data = (*env)->GetByteArrayElements(env, ciphertext, NULL);
        ciphertext_buffer.data = (uint8_t*)ciphertext_data;
    }
    
    // Convert nonce byte array to ByteBuffer (optional)
    struct ByteBuffer nonce_buffer = {0};
    jbyte* nonce_data = NULL;
    if (nonce != NULL) {
        jsize nonce_len = (*env)->GetArrayLength(env, nonce);
        nonce_buffer.len = (int64_t)nonce_len;
        if (nonce_len > 0) {
            nonce_data = (*env)->GetByteArrayElements(env, nonce, NULL);
            nonce_buffer.data = (uint8_t*)nonce_data;
        }
    }
    
    // Convert tag byte array to ByteBuffer (optional)
    struct ByteBuffer tag_buffer = {0};
    jbyte* tag_data = NULL;
    if (tag != NULL) {
        jsize tag_len = (*env)->GetArrayLength(env, tag);
        tag_buffer.len = (int64_t)tag_len;
        if (tag_len > 0) {
            tag_data = (*env)->GetByteArrayElements(env, tag, NULL);
            tag_buffer.data = (uint8_t*)tag_data;
        }
    }
    
    // Convert aad byte array to ByteBuffer (optional)
    struct ByteBuffer aad_buffer = {0};
    jbyte* aad_data = NULL;
    if (aad != NULL) {
        jsize aad_len = (*env)->GetArrayLength(env, aad);
        aad_buffer.len = (int64_t)aad_len;
        if (aad_len > 0) {
            aad_data = (*env)->GetByteArrayElements(env, aad, NULL);
            aad_buffer.data = (uint8_t*)aad_data;
        }
    }
    
    printf("DEBUG: keyAeadDecrypt called with handle=%ld, ciphertext_len=%ld, nonce_len=%ld, tag_len=%ld, aad_len=%ld\n",
            keyHandle, ciphertext_buffer.len, nonce_buffer.len, tag_buffer.len, aad_buffer.len);
    fflush(stdout);
    
    LocalKeyHandle handle = {(const struct LocalKey*)keyHandle};
    struct SecretBuffer decrypted_buffer = {0};
    
    ErrorCode result = askar_key_aead_decrypt(handle, ciphertext_buffer, nonce_buffer, tag_buffer, aad_buffer, &decrypted_buffer);
    
    if (ciphertext_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, ciphertext, ciphertext_data, JNI_ABORT);
    }
    if (nonce_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, nonce, nonce_data, JNI_ABORT);
    }
    if (tag_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, tag, tag_data, JNI_ABORT);
    }
    if (aad_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, aad, aad_data, JNI_ABORT);
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: askar_key_aead_decrypt returned error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[512];
            if (error_json) {
                snprintf(error_msg, sizeof(error_msg), "AEAD decryption failed with error code: %ld, detail: %s", result, error_json);
            } else {
                snprintf(error_msg, sizeof(error_msg), "AEAD decryption failed with error code: %ld", result);
            }
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return NULL;
    }
    
    if (decrypted_buffer.data == NULL || decrypted_buffer.len <= 0) {
        printf("DEBUG: decrypted buffer is empty\n");
        return NULL;
    }
    
    jbyteArray jdecrypted = (*env)->NewByteArray(env, (jsize)decrypted_buffer.len);
    if (jdecrypted == NULL) {
        return NULL;
    }
    
    (*env)->SetByteArrayRegion(env, jdecrypted, 0, (jsize)decrypted_buffer.len, (jbyte*)decrypted_buffer.data);
    printf("DEBUG: keyAeadDecrypt succeeded, decrypted_len=%ld\n", decrypted_buffer.len);
    return jdecrypted;
}

JNIEXPORT jbyteArray JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyCryptoBox
  (JNIEnv *env, jclass cls, jlong recipientKey, jlong senderKey, jbyteArray message, jbyteArray nonce) {
    
    if (recipientKey == 0 || senderKey == 0 || message == NULL) {
        return NULL;
    }
    
    // Convert message byte array to ByteBuffer
    struct ByteBuffer message_buffer = {0};
    jbyte* message_data = NULL;
    jsize message_len = (*env)->GetArrayLength(env, message);
    message_buffer.len = (int64_t)message_len;
    if (message_len > 0) {
        message_data = (*env)->GetByteArrayElements(env, message, NULL);
        message_buffer.data = (uint8_t*)message_data;
    }
    
    // Convert nonce byte array to ByteBuffer (optional)
    struct ByteBuffer nonce_buffer = {0};
    jbyte* nonce_data = NULL;
    if (nonce != NULL) {
        jsize nonce_len = (*env)->GetArrayLength(env, nonce);
        nonce_buffer.len = (int64_t)nonce_len;
        if (nonce_len > 0) {
            nonce_data = (*env)->GetByteArrayElements(env, nonce, NULL);
            nonce_buffer.data = (uint8_t*)nonce_data;
        }
    }
    
    printf("DEBUG: keyCryptoBox called with recipientKey=%ld, senderKey=%ld, message_len=%ld, nonce_len=%ld\n",
            recipientKey, senderKey, message_buffer.len, nonce_buffer.len);
    fflush(stdout);
    
    LocalKeyHandle recip_handle = {(const struct LocalKey*)recipientKey};
    LocalKeyHandle sender_handle = {(const struct LocalKey*)senderKey};
    struct SecretBuffer encrypted_buffer = {0};
    
    ErrorCode result = askar_key_crypto_box(recip_handle, sender_handle, message_buffer, nonce_buffer, &encrypted_buffer);
    
    if (message_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, message, message_data, JNI_ABORT);
    }
    if (nonce_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, nonce, nonce_data, JNI_ABORT);
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: askar_key_crypto_box returned error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[512];
            if (error_json) {
                snprintf(error_msg, sizeof(error_msg), "Crypto box encryption failed with error code: %ld, detail: %s", result, error_json);
            } else {
                snprintf(error_msg, sizeof(error_msg), "Crypto box encryption failed with error code: %ld", result);
            }
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return NULL;
    }
    
    if (encrypted_buffer.data == NULL || encrypted_buffer.len <= 0) {
        printf("DEBUG: crypto box encrypted buffer is empty\n");
        return NULL;
    }
    
    jbyteArray jencrypted = (*env)->NewByteArray(env, (jsize)encrypted_buffer.len);
    if (jencrypted == NULL) {
        return NULL;
    }
    
    (*env)->SetByteArrayRegion(env, jencrypted, 0, (jsize)encrypted_buffer.len, (jbyte*)encrypted_buffer.data);
    printf("DEBUG: keyCryptoBox succeeded, encrypted_len=%ld\n", encrypted_buffer.len);
    return jencrypted;
}

JNIEXPORT jbyteArray JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyCryptoBoxOpen
  (JNIEnv *env, jclass cls, jlong recipientKey, jlong senderKey, jbyteArray ciphertext, jbyteArray nonce) {
    
    if (recipientKey == 0 || senderKey == 0 || ciphertext == NULL) {
        return NULL;
    }
    
    // Convert ciphertext byte array to ByteBuffer
    struct ByteBuffer ciphertext_buffer = {0};
    jbyte* ciphertext_data = NULL;
    jsize ciphertext_len = (*env)->GetArrayLength(env, ciphertext);
    ciphertext_buffer.len = (int64_t)ciphertext_len;
    if (ciphertext_len > 0) {
        ciphertext_data = (*env)->GetByteArrayElements(env, ciphertext, NULL);
        ciphertext_buffer.data = (uint8_t*)ciphertext_data;
    }
    
    // Convert nonce byte array to ByteBuffer (optional)
    struct ByteBuffer nonce_buffer = {0};
    jbyte* nonce_data = NULL;
    if (nonce != NULL) {
        jsize nonce_len = (*env)->GetArrayLength(env, nonce);
        nonce_buffer.len = (int64_t)nonce_len;
        if (nonce_len > 0) {
            nonce_data = (*env)->GetByteArrayElements(env, nonce, NULL);
            nonce_buffer.data = (uint8_t*)nonce_data;
        }
    }
    
    printf("DEBUG: keyCryptoBoxOpen called with recipientKey=%ld, senderKey=%ld, ciphertext_len=%ld, nonce_len=%ld\n",
            recipientKey, senderKey, ciphertext_buffer.len, nonce_buffer.len);
    fflush(stdout);
    
    LocalKeyHandle recip_handle = {(const struct LocalKey*)recipientKey};
    LocalKeyHandle sender_handle = {(const struct LocalKey*)senderKey};
    struct SecretBuffer decrypted_buffer = {0};
    
    ErrorCode result = askar_key_crypto_box_open(recip_handle, sender_handle, ciphertext_buffer, nonce_buffer, &decrypted_buffer);
    
    if (ciphertext_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, ciphertext, ciphertext_data, JNI_ABORT);
    }
    if (nonce_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, nonce, nonce_data, JNI_ABORT);
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: askar_key_crypto_box_open returned error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[512];
            if (error_json) {
                snprintf(error_msg, sizeof(error_msg), "Crypto box decryption failed with error code: %ld, detail: %s", result, error_json);
            } else {
                snprintf(error_msg, sizeof(error_msg), "Crypto box decryption failed with error code: %ld", result);
            }
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return NULL;
    }
    
    if (decrypted_buffer.data == NULL || decrypted_buffer.len <= 0) {
        printf("DEBUG: crypto box decrypted buffer is empty\n");
        return NULL;
    }
    
    jbyteArray jdecrypted = (*env)->NewByteArray(env, (jsize)decrypted_buffer.len);
    if (jdecrypted == NULL) {
        return NULL;
    }
    
    (*env)->SetByteArrayRegion(env, jdecrypted, 0, (jsize)decrypted_buffer.len, (jbyte*)decrypted_buffer.data);
    printf("DEBUG: keyCryptoBoxOpen succeeded, decrypted_len=%ld\n", decrypted_buffer.len);
    return jdecrypted;
}

JNIEXPORT jbyteArray JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyWrapKey
  (JNIEnv *env, jclass cls, jlong wrapperKey, jlong keyToWrap, jbyteArray nonce) {
    
    if (wrapperKey == 0 || keyToWrap == 0) {
        return NULL;
    }
    
    // Convert nonce byte array to ByteBuffer (optional)
    struct ByteBuffer nonce_buffer = {0};
    jbyte* nonce_data = NULL;
    if (nonce != NULL) {
        jsize nonce_len = (*env)->GetArrayLength(env, nonce);
        nonce_buffer.len = (int64_t)nonce_len;
        if (nonce_len > 0) {
            nonce_data = (*env)->GetByteArrayElements(env, nonce, NULL);
            nonce_buffer.data = (uint8_t*)nonce_data;
        }
    }
    
    printf("DEBUG: keyWrapKey called with wrapperKey=%ld, keyToWrap=%ld, nonce_len=%ld\n",
            wrapperKey, keyToWrap, nonce_buffer.len);
    fflush(stdout);
    
    LocalKeyHandle wrapper_handle = {(const struct LocalKey*)wrapperKey};
    LocalKeyHandle other_handle = {(const struct LocalKey*)keyToWrap};
    struct EncryptedBuffer wrapped_buffer = {0};
    
    ErrorCode result = askar_key_wrap_key(wrapper_handle, other_handle, nonce_buffer, &wrapped_buffer);
    
    if (nonce_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, nonce, nonce_data, JNI_ABORT);
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: askar_key_wrap_key returned error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[512];
            if (error_json) {
                snprintf(error_msg, sizeof(error_msg), "Key wrapping failed with error code: %ld, detail: %s", result, error_json);
            } else {
                snprintf(error_msg, sizeof(error_msg), "Key wrapping failed with error code: %ld", result);
            }
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return NULL;
    }
    
    if (wrapped_buffer.buffer.data == NULL || wrapped_buffer.buffer.len <= 0) {
        printf("DEBUG: wrapped key buffer is empty\n");
        return NULL;
    }
    
    jbyteArray jwrapped = (*env)->NewByteArray(env, (jsize)wrapped_buffer.buffer.len);
    if (jwrapped == NULL) {
        return NULL;
    }
    
    (*env)->SetByteArrayRegion(env, jwrapped, 0, (jsize)wrapped_buffer.buffer.len, (jbyte*)wrapped_buffer.buffer.data);
    printf("DEBUG: keyWrapKey succeeded, wrapped_len=%ld, tag_pos=%ld, nonce_pos=%ld\n", 
           wrapped_buffer.buffer.len, wrapped_buffer.tag_pos, wrapped_buffer.nonce_pos);
    return jwrapped;
}

JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyUnwrapKey
  (JNIEnv *env, jclass cls, jlong wrapperKey, jstring algorithm, jbyteArray ciphertext, jbyteArray nonce, jbyteArray tag) {
    
    if (wrapperKey == 0 || ciphertext == NULL) {
        return 0;
    }
    
    const char* c_algorithm = get_string_utf(env, algorithm);
    
    // Convert ciphertext byte array to ByteBuffer
    struct ByteBuffer ciphertext_buffer = {0};
    jbyte* ciphertext_data = NULL;
    jsize ciphertext_len = (*env)->GetArrayLength(env, ciphertext);
    ciphertext_buffer.len = (int64_t)ciphertext_len;
    if (ciphertext_len > 0) {
        ciphertext_data = (*env)->GetByteArrayElements(env, ciphertext, NULL);
        ciphertext_buffer.data = (uint8_t*)ciphertext_data;
    }
    
    // Convert nonce byte array to ByteBuffer (optional)
    struct ByteBuffer nonce_buffer = {0};
    jbyte* nonce_data = NULL;
    if (nonce != NULL) {
        jsize nonce_len = (*env)->GetArrayLength(env, nonce);
        nonce_buffer.len = (int64_t)nonce_len;
        if (nonce_len > 0) {
            nonce_data = (*env)->GetByteArrayElements(env, nonce, NULL);
            nonce_buffer.data = (uint8_t*)nonce_data;
        }
    }
    
    // Convert tag byte array to ByteBuffer (optional)
    struct ByteBuffer tag_buffer = {0};
    jbyte* tag_data = NULL;
    if (tag != NULL) {
        jsize tag_len = (*env)->GetArrayLength(env, tag);
        tag_buffer.len = (int64_t)tag_len;
        if (tag_len > 0) {
            tag_data = (*env)->GetByteArrayElements(env, tag, NULL);
            tag_buffer.data = (uint8_t*)tag_data;
        }
    }
    
    printf("DEBUG: keyUnwrapKey called with wrapperKey=%ld, algorithm=%s, ciphertext_len=%ld, nonce_len=%ld, tag_len=%ld\n",
            wrapperKey, c_algorithm ? c_algorithm : "NULL", ciphertext_buffer.len, nonce_buffer.len, tag_buffer.len);
    fflush(stdout);
    
    LocalKeyHandle wrapper_handle = {(const struct LocalKey*)wrapperKey};
    LocalKeyHandle unwrapped_key = {NULL};
    
    ErrorCode result = askar_key_unwrap_key(wrapper_handle, c_algorithm, ciphertext_buffer, nonce_buffer, tag_buffer, &unwrapped_key);
    
    release_string_utf(env, algorithm, c_algorithm);
    if (ciphertext_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, ciphertext, ciphertext_data, JNI_ABORT);
    }
    if (nonce_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, nonce, nonce_data, JNI_ABORT);
    }
    if (tag_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, tag, tag_data, JNI_ABORT);
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: askar_key_unwrap_key returned error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[512];
            if (error_json) {
                snprintf(error_msg, sizeof(error_msg), "Key unwrapping failed with error code: %ld, detail: %s", result, error_json);
            } else {
                snprintf(error_msg, sizeof(error_msg), "Key unwrapping failed with error code: %ld", result);
            }
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return 0;
    }
    
    jlong handle_value = (jlong)(uintptr_t)unwrapped_key._0;
    printf("DEBUG: keyUnwrapKey succeeded, unwrapped keyHandle=%ld\n", handle_value);
    return handle_value;
}

// Key derivation operations
JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyDeriveEcdhEs
  (JNIEnv *env, jclass cls, jstring algorithm, jlong ephemeralKey, jlong recipientKey, 
   jbyteArray algorithmId, jbyteArray apu, jbyteArray apv, jboolean receive) {
    
    if (ephemeralKey == 0 || recipientKey == 0) {
        return 0;
    }
    
    const char* c_algorithm = get_string_utf(env, algorithm);
    
    // Convert algorithmId byte array to ByteBuffer
    struct ByteBuffer alg_id_buffer = {0};
    jbyte* alg_id_data = NULL;
    if (algorithmId != NULL) {
        jsize len = (*env)->GetArrayLength(env, algorithmId);
        alg_id_buffer.len = (int64_t)len;
        if (len > 0) {
            alg_id_data = (*env)->GetByteArrayElements(env, algorithmId, NULL);
            alg_id_buffer.data = (uint8_t*)alg_id_data;
        }
    }
    
    // Convert apu byte array to ByteBuffer
    struct ByteBuffer apu_buffer = {0};
    jbyte* apu_data = NULL;
    if (apu != NULL) {
        jsize len = (*env)->GetArrayLength(env, apu);
        apu_buffer.len = (int64_t)len;
        if (len > 0) {
            apu_data = (*env)->GetByteArrayElements(env, apu, NULL);
            apu_buffer.data = (uint8_t*)apu_data;
        }
    }
    
    // Convert apv byte array to ByteBuffer
    struct ByteBuffer apv_buffer = {0};
    jbyte* apv_data = NULL;
    if (apv != NULL) {
        jsize len = (*env)->GetArrayLength(env, apv);
        apv_buffer.len = (int64_t)len;
        if (len > 0) {
            apv_data = (*env)->GetByteArrayElements(env, apv, NULL);
            apv_buffer.data = (uint8_t*)apv_data;
        }
    }
    
    printf("DEBUG: keyDeriveEcdhEs called with algorithm=%s, ephemeral=%ld, recipient=%ld, alg_id_len=%ld, apu_len=%ld, apv_len=%ld, receive=%d\n",
            c_algorithm ? c_algorithm : "NULL", ephemeralKey, recipientKey, 
            alg_id_buffer.len, apu_buffer.len, apv_buffer.len, receive);
    fflush(stdout);
    
    LocalKeyHandle ephem_handle = {(const struct LocalKey*)ephemeralKey};
    LocalKeyHandle recip_handle = {(const struct LocalKey*)recipientKey};
    LocalKeyHandle derived_key = {NULL};
    
    ErrorCode result = askar_key_derive_ecdh_es(c_algorithm, ephem_handle, recip_handle,
                                                alg_id_buffer, apu_buffer, apv_buffer,
                                                (int8_t)receive, &derived_key);
    
    release_string_utf(env, algorithm, c_algorithm);
    if (alg_id_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, algorithmId, alg_id_data, JNI_ABORT);
    }
    if (apu_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, apu, apu_data, JNI_ABORT);
    }
    if (apv_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, apv, apv_data, JNI_ABORT);
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: askar_key_derive_ecdh_es returned error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[512];
            if (error_json) {
                snprintf(error_msg, sizeof(error_msg), "ECDH-ES key derivation failed with error code: %ld, detail: %s", result, error_json);
            } else {
                snprintf(error_msg, sizeof(error_msg), "ECDH-ES key derivation failed with error code: %ld", result);
            }
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return 0;
    }
    
    jlong handle_value = (jlong)(uintptr_t)derived_key._0;
    printf("DEBUG: keyDeriveEcdhEs succeeded, derived keyHandle=%ld\n", handle_value);
    return handle_value;
}

JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyDeriveEcdh1Pu
  (JNIEnv *env, jclass cls, jstring algorithm, jlong ephemeralKey, jlong senderKey, jlong recipientKey,
   jbyteArray algorithmId, jbyteArray apu, jbyteArray apv, jbyteArray ccTag, jboolean receive) {
    
    if (ephemeralKey == 0 || senderKey == 0 || recipientKey == 0) {
        return 0;
    }
    
    const char* c_algorithm = get_string_utf(env, algorithm);
    
    // Convert algorithmId byte array to ByteBuffer
    struct ByteBuffer alg_id_buffer = {0};
    jbyte* alg_id_data = NULL;
    if (algorithmId != NULL) {
        jsize len = (*env)->GetArrayLength(env, algorithmId);
        alg_id_buffer.len = (int64_t)len;
        if (len > 0) {
            alg_id_data = (*env)->GetByteArrayElements(env, algorithmId, NULL);
            alg_id_buffer.data = (uint8_t*)alg_id_data;
        }
    }
    
    // Convert apu byte array to ByteBuffer
    struct ByteBuffer apu_buffer = {0};
    jbyte* apu_data = NULL;
    if (apu != NULL) {
        jsize len = (*env)->GetArrayLength(env, apu);
        apu_buffer.len = (int64_t)len;
        if (len > 0) {
            apu_data = (*env)->GetByteArrayElements(env, apu, NULL);
            apu_buffer.data = (uint8_t*)apu_data;
        }
    }
    
    // Convert apv byte array to ByteBuffer
    struct ByteBuffer apv_buffer = {0};
    jbyte* apv_data = NULL;
    if (apv != NULL) {
        jsize len = (*env)->GetArrayLength(env, apv);
        apv_buffer.len = (int64_t)len;
        if (len > 0) {
            apv_data = (*env)->GetByteArrayElements(env, apv, NULL);
            apv_buffer.data = (uint8_t*)apv_data;
        }
    }
    
    // Convert ccTag byte array to ByteBuffer
    struct ByteBuffer cc_tag_buffer = {0};
    jbyte* cc_tag_data = NULL;
    if (ccTag != NULL) {
        jsize len = (*env)->GetArrayLength(env, ccTag);
        cc_tag_buffer.len = (int64_t)len;
        if (len > 0) {
            cc_tag_data = (*env)->GetByteArrayElements(env, ccTag, NULL);
            cc_tag_buffer.data = (uint8_t*)cc_tag_data;
        }
    }
    
    printf("DEBUG: keyDeriveEcdh1Pu called with algorithm=%s, ephemeral=%ld, sender=%ld, recipient=%ld, alg_id_len=%ld, apu_len=%ld, apv_len=%ld, cc_tag_len=%ld, receive=%d\n",
            c_algorithm ? c_algorithm : "NULL", ephemeralKey, senderKey, recipientKey,
            alg_id_buffer.len, apu_buffer.len, apv_buffer.len, cc_tag_buffer.len, receive);
    fflush(stdout);
    
    LocalKeyHandle ephem_handle = {(const struct LocalKey*)ephemeralKey};
    LocalKeyHandle sender_handle = {(const struct LocalKey*)senderKey};
    LocalKeyHandle recip_handle = {(const struct LocalKey*)recipientKey};
    LocalKeyHandle derived_key = {NULL};
    
    ErrorCode result = askar_key_derive_ecdh_1pu(c_algorithm, ephem_handle, sender_handle, recip_handle,
                                                 alg_id_buffer, apu_buffer, apv_buffer, cc_tag_buffer,
                                                 (int8_t)receive, &derived_key);
    
    release_string_utf(env, algorithm, c_algorithm);
    if (alg_id_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, algorithmId, alg_id_data, JNI_ABORT);
    }
    if (apu_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, apu, apu_data, JNI_ABORT);
    }
    if (apv_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, apv, apv_data, JNI_ABORT);
    }
    if (cc_tag_data != NULL) {
        (*env)->ReleaseByteArrayElements(env, ccTag, cc_tag_data, JNI_ABORT);
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: askar_key_derive_ecdh_1pu returned error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[512];
            if (error_json) {
                snprintf(error_msg, sizeof(error_msg), "ECDH-1PU key derivation failed with error code: %ld, detail: %s", result, error_json);
            } else {
                snprintf(error_msg, sizeof(error_msg), "ECDH-1PU key derivation failed with error code: %ld", result);
            }
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return 0;
    }
    
    jlong handle_value = (jlong)(uintptr_t)derived_key._0;
    printf("DEBUG: keyDeriveEcdh1Pu succeeded, derived keyHandle=%ld\n", handle_value);
    return handle_value;
}

JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyFromKeyExchange
  (JNIEnv *env, jclass cls, jstring algorithm, jlong secretKey, jlong publicKey) {
    
    if (secretKey == 0 || publicKey == 0) {
        return 0;
    }
    
    const char* c_algorithm = get_string_utf(env, algorithm);
    
    printf("DEBUG: keyFromKeyExchange called with algorithm=%s, secretKey=%ld, publicKey=%ld\n",
            c_algorithm ? c_algorithm : "NULL", secretKey, publicKey);
    fflush(stdout);
    
    LocalKeyHandle sk_handle = {(const struct LocalKey*)secretKey};
    LocalKeyHandle pk_handle = {(const struct LocalKey*)publicKey};
    LocalKeyHandle derived_key = {NULL};
    
    ErrorCode result = askar_key_from_key_exchange(c_algorithm, sk_handle, pk_handle, &derived_key);
    
    release_string_utf(env, algorithm, c_algorithm);
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: askar_key_from_key_exchange returned error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[512];
            if (error_json) {
                snprintf(error_msg, sizeof(error_msg), "Key exchange failed with error code: %ld, detail: %s", result, error_json);
            } else {
                snprintf(error_msg, sizeof(error_msg), "Key exchange failed with error code: %ld", result);
            }
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return 0;
    }
    
    jlong handle_value = (jlong)(uintptr_t)derived_key._0;
    printf("DEBUG: keyFromKeyExchange succeeded, derived keyHandle=%ld\n", handle_value);
    return handle_value;
}

JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyConvert
  (JNIEnv *env, jclass cls, jlong keyHandle, jstring algorithm) {
    
    if (keyHandle == 0) {
        return 0;
    }
    
    const char* c_algorithm = get_string_utf(env, algorithm);
    
    printf("DEBUG: keyConvert called with keyHandle=%ld, algorithm=%s\n",
            keyHandle, c_algorithm ? c_algorithm : "NULL");
    fflush(stdout);
    
    LocalKeyHandle handle = {(const struct LocalKey*)keyHandle};
    LocalKeyHandle converted_key = {NULL};
    
    ErrorCode result = askar_key_convert(handle, c_algorithm, &converted_key);
    
    release_string_utf(env, algorithm, c_algorithm);
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: askar_key_convert returned error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[512];
            if (error_json) {
                snprintf(error_msg, sizeof(error_msg), "Key conversion failed with error code: %ld, detail: %s", result, error_json);
            } else {
                snprintf(error_msg, sizeof(error_msg), "Key conversion failed with error code: %ld", result);
            }
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return 0;
    }
    
    jlong handle_value = (jlong)(uintptr_t)converted_key._0;
    printf("DEBUG: keyConvert succeeded, converted keyHandle=%ld\n", handle_value);
    return handle_value;
}

JNIEXPORT jbyteArray JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyAeadRandomNonce
  (JNIEnv *env, jclass cls, jlong keyHandle) {
    
    if (keyHandle == 0) {
        return NULL;
    }
    
    printf("DEBUG: keyAeadRandomNonce called with keyHandle=%ld\n", keyHandle);
    fflush(stdout);
    
    LocalKeyHandle handle = {(const struct LocalKey*)keyHandle};
    struct SecretBuffer nonce_buffer = {0};
    
    ErrorCode result = askar_key_aead_random_nonce(handle, &nonce_buffer);
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: askar_key_aead_random_nonce returned error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        return NULL;
    }
    
    if (nonce_buffer.data == NULL || nonce_buffer.len <= 0) {
        printf("DEBUG: random nonce buffer is empty\n");
        return NULL;
    }
    
    jbyteArray jnonce = (*env)->NewByteArray(env, (jsize)nonce_buffer.len);
    if (jnonce == NULL) {
        return NULL;
    }
    
    (*env)->SetByteArrayRegion(env, jnonce, 0, (jsize)nonce_buffer.len, (jbyte*)nonce_buffer.data);
    printf("DEBUG: keyAeadRandomNonce succeeded, nonce_len=%ld\n", nonce_buffer.len);
    return jnonce;
}

JNIEXPORT jbyteArray JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyCryptoBoxRandomNonce
  (JNIEnv *env, jclass cls) {
    
    printf("DEBUG: keyCryptoBoxRandomNonce called\n");
    fflush(stdout);
    
    struct SecretBuffer nonce_buffer = {0};
    
    ErrorCode result = askar_key_crypto_box_random_nonce(&nonce_buffer);
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: askar_key_crypto_box_random_nonce returned error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        return NULL;
    }
    
    if (nonce_buffer.data == NULL || nonce_buffer.len <= 0) {
        printf("DEBUG: crypto box random nonce buffer is empty\n");
        return NULL;
    }
    
    jbyteArray jnonce = (*env)->NewByteArray(env, (jsize)nonce_buffer.len);
    if (jnonce == NULL) {
        return NULL;
    }
    
    (*env)->SetByteArrayRegion(env, jnonce, 0, (jsize)nonce_buffer.len, (jbyte*)nonce_buffer.data);
    printf("DEBUG: keyCryptoBoxRandomNonce succeeded, nonce_len=%ld\n", nonce_buffer.len);
    return jnonce;
}

// ========== Store Management Operations ==========

// Callback for store operations that return void
void store_mgmt_void_callback(int64_t callback_id, ErrorCode error_code) {
    pthread_mutex_lock(&callback_mutex);
    callback_result = 0; // No meaningful result for void operations
    callback_error = error_code;
    callback_completed = 1;
    pthread_cond_signal(&callback_cond);
    pthread_mutex_unlock(&callback_mutex);
}

// Callback for store operations that return boolean
void store_mgmt_bool_callback(int64_t callback_id, ErrorCode error_code, int8_t result) {
    pthread_mutex_lock(&callback_mutex);
    callback_result = (int64_t)result;
    callback_error = error_code;
    callback_completed = 1;
    pthread_cond_signal(&callback_cond);
    pthread_mutex_unlock(&callback_mutex);
}

// Callback for store operations that return string
void store_mgmt_string_callback(int64_t callback_id, ErrorCode error_code, const char *result_str) {
    pthread_mutex_lock(&callback_mutex);
    // Store string pointer as callback result (caller must handle properly)
    callback_result = (int64_t)(uintptr_t)result_str;
    callback_error = error_code;
    callback_completed = 1;
    pthread_cond_signal(&callback_cond);
    pthread_mutex_unlock(&callback_mutex);
}

// Store rekey
JNIEXPORT void JNICALL Java_org_hyperledger_aries_askar_AskarNative_storeRekey
  (JNIEnv *env, jclass cls, jlong storeHandle, jstring keyMethod, jstring passKey) {
    printf("DEBUG: storeRekey called with storeHandle=%ld\n", storeHandle);
    
    const char* key_method_str = get_string_utf(env, keyMethod);
    const char* pass_key_str = get_string_utf(env, passKey);
    
    StoreHandle store = (StoreHandle)storeHandle;
    ErrorCode result = askar_store_rekey(store, key_method_str, pass_key_str, store_mgmt_void_callback, 1);
    
    if (result == 0) {
        result = wait_for_callback();
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: storeRekey failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        // Release strings before throwing
        release_string_utf(env, keyMethod, key_method_str);
        release_string_utf(env, passKey, pass_key_str);
        
        jclass exception_class = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exception_class) {
            char error_msg[256];
            snprintf(error_msg, sizeof(error_msg), "Store rekey failed with error code: %ld", result);
            (*env)->ThrowNew(env, exception_class, error_msg);
        }
        return;
    }
    
    release_string_utf(env, keyMethod, key_method_str);
    release_string_utf(env, passKey, pass_key_str);
    printf("DEBUG: storeRekey completed successfully\n");
}

// Store remove
JNIEXPORT jboolean JNICALL Java_org_hyperledger_aries_askar_AskarNative_storeRemove
  (JNIEnv *env, jclass cls, jstring uri) {
    printf("DEBUG: storeRemove called\n");
    
    const char* uri_str = get_string_utf(env, uri);
    if (uri_str == NULL) {
        return JNI_FALSE;
    }
    
    ErrorCode result = askar_store_remove(uri_str, store_mgmt_bool_callback, 1);
    
    if (result == 0) {
        result = wait_for_callback();
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: storeRemove failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        release_string_utf(env, uri, uri_str);
        return JNI_FALSE;
    }
    
    jboolean removed = (callback_result != 0) ? JNI_TRUE : JNI_FALSE;
    release_string_utf(env, uri, uri_str);
    printf("DEBUG: storeRemove completed, removed=%s\n", removed ? "true" : "false");
    return removed;
}

// Store copy to
JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_storeCopyTo
  (JNIEnv *env, jclass cls, jlong storeHandle, jstring targetUri, jstring keyMethod, jstring passKey, jboolean recreate) {
    printf("DEBUG: storeCopyTo called with storeHandle=%ld, recreate=%s\n", storeHandle, recreate ? "true" : "false");
    
    const char* target_uri_str = get_string_utf(env, targetUri);
    const char* key_method_str = get_string_utf(env, keyMethod);
    const char* pass_key_str = get_string_utf(env, passKey);
    
    StoreHandle source_store = (StoreHandle)storeHandle;
    int8_t recreate_flag = recreate ? 1 : 0;
    
    ErrorCode result = askar_store_copy(source_store, target_uri_str, key_method_str, pass_key_str, 
                                       recreate_flag, store_handle_callback, 1);
    
    if (result == 0) {
        result = wait_for_callback();
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: storeCopyTo failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        // Release strings before returning
        release_string_utf(env, targetUri, target_uri_str);
        release_string_utf(env, keyMethod, key_method_str);
        release_string_utf(env, passKey, pass_key_str);
        return 0;
    }
    
    jlong new_store_handle = (jlong)callback_result;
    release_string_utf(env, targetUri, target_uri_str);
    release_string_utf(env, keyMethod, key_method_str);
    release_string_utf(env, passKey, pass_key_str);
    printf("DEBUG: storeCopyTo completed successfully, new store handle=%ld\n", new_store_handle);
    return new_store_handle;
}

// Store create profile
JNIEXPORT jstring JNICALL Java_org_hyperledger_aries_askar_AskarNative_storeCreateProfile
  (JNIEnv *env, jclass cls, jlong storeHandle, jstring profile) {
    printf("DEBUG: storeCreateProfile called with storeHandle=%ld\n", storeHandle);
    
    const char* profile_str = get_string_utf(env, profile);
    if (profile_str == NULL) {
        return NULL;
    }
    
    StoreHandle store = (StoreHandle)storeHandle;
    ErrorCode result = askar_store_create_profile(store, profile_str, store_mgmt_string_callback, 1);
    
    if (result == 0) {
        result = wait_for_callback();
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: storeCreateProfile failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        release_string_utf(env, profile, profile_str);
        return NULL;
    }
    
    const char* result_profile = (const char*)(uintptr_t)callback_result;
    jstring jresult = NULL;
    if (result_profile) {
        jresult = (*env)->NewStringUTF(env, result_profile);
    }
    
    release_string_utf(env, profile, profile_str);
    printf("DEBUG: storeCreateProfile completed successfully\n");
    return jresult;
}

// Store remove profile
JNIEXPORT jboolean JNICALL Java_org_hyperledger_aries_askar_AskarNative_storeRemoveProfile
  (JNIEnv *env, jclass cls, jlong storeHandle, jstring profile) {
    printf("DEBUG: storeRemoveProfile called with storeHandle=%ld\n", storeHandle);
    
    const char* profile_str = get_string_utf(env, profile);
    if (profile_str == NULL) {
        return JNI_FALSE;
    }
    
    StoreHandle store = (StoreHandle)storeHandle;
    ErrorCode result = askar_store_remove_profile(store, profile_str, store_mgmt_bool_callback, 1);
    
    if (result == 0) {
        result = wait_for_callback();
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: storeRemoveProfile failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        release_string_utf(env, profile, profile_str);
        return JNI_FALSE;
    }
    
    jboolean removed = (callback_result != 0) ? JNI_TRUE : JNI_FALSE;
    release_string_utf(env, profile, profile_str);
    printf("DEBUG: storeRemoveProfile completed, removed=%s\n", removed ? "true" : "false");
    return removed;
}

// Store get profile name
JNIEXPORT jstring JNICALL Java_org_hyperledger_aries_askar_AskarNative_storeGetProfileName
  (JNIEnv *env, jclass cls, jlong storeHandle) {
    printf("DEBUG: storeGetProfileName called with storeHandle=%ld\n", storeHandle);
    
    StoreHandle store = (StoreHandle)storeHandle;
    ErrorCode result = askar_store_get_profile_name(store, store_mgmt_string_callback, 1);
    
    if (result == 0) {
        result = wait_for_callback();
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: storeGetProfileName failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        return NULL;
    }
    
    const char* profile_name = (const char*)(uintptr_t)callback_result;
    jstring jresult = NULL;
    if (profile_name) {
        jresult = (*env)->NewStringUTF(env, profile_name);
    }
    
    printf("DEBUG: storeGetProfileName completed successfully\n");
    return jresult;
}

// Store get default profile
JNIEXPORT jstring JNICALL Java_org_hyperledger_aries_askar_AskarNative_storeGetDefaultProfile
  (JNIEnv *env, jclass cls, jlong storeHandle) {
    printf("DEBUG: storeGetDefaultProfile called with storeHandle=%ld\n", storeHandle);
    
    StoreHandle store = (StoreHandle)storeHandle;
    ErrorCode result = askar_store_get_default_profile(store, store_mgmt_string_callback, 1);
    
    if (result == 0) {
        result = wait_for_callback();
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: storeGetDefaultProfile failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        return NULL;
    }
    
    const char* profile_name = (const char*)(uintptr_t)callback_result;
    jstring jresult = NULL;
    if (profile_name) {
        jresult = (*env)->NewStringUTF(env, profile_name);
    }
    
    printf("DEBUG: storeGetDefaultProfile completed successfully\n");
    return jresult;
}

// Store set default profile
JNIEXPORT void JNICALL Java_org_hyperledger_aries_askar_AskarNative_storeSetDefaultProfile
  (JNIEnv *env, jclass cls, jlong storeHandle, jstring profile) {
    printf("DEBUG: storeSetDefaultProfile called with storeHandle=%ld\n", storeHandle);
    
    const char* profile_str = get_string_utf(env, profile);
    if (profile_str == NULL) {
        return;
    }
    
    StoreHandle store = (StoreHandle)storeHandle;
    ErrorCode result = askar_store_set_default_profile(store, profile_str, store_mgmt_void_callback, 1);
    
    if (result == 0) {
        result = wait_for_callback();
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: storeSetDefaultProfile failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        release_string_utf(env, profile, profile_str);
        
        jclass exception_class = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exception_class) {
            char error_msg[256];
            snprintf(error_msg, sizeof(error_msg), "Set default profile failed with error code: %ld", result);
            (*env)->ThrowNew(env, exception_class, error_msg);
        }
        return;
    }
    
    release_string_utf(env, profile, profile_str);
    printf("DEBUG: storeSetDefaultProfile completed successfully\n");
}

// Helper callback for string list operations
void string_list_callback(int64_t callback_id, ErrorCode error_code, StringListHandle string_list_handle) {
    pthread_mutex_lock(&callback_mutex);
    callback_result = (int64_t)(uintptr_t)string_list_handle._0;
    callback_error = error_code;
    callback_completed = 1;
    pthread_cond_signal(&callback_cond);
    pthread_mutex_unlock(&callback_mutex);
}

// Store list profiles
JNIEXPORT jobjectArray JNICALL Java_org_hyperledger_aries_askar_AskarNative_storeListProfiles
  (JNIEnv *env, jclass cls, jlong storeHandle) {
    printf("DEBUG: storeListProfiles called with storeHandle=%ld\n", storeHandle);
    
    StoreHandle store = (StoreHandle)storeHandle;
    ErrorCode result = askar_store_list_profiles(store, string_list_callback, 1);
    
    if (result == 0) {
        result = wait_for_callback();
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: storeListProfiles failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        return NULL;
    }
    
    StringListHandle string_list;
    string_list._0 = (FfiStringList*)(uintptr_t)callback_result;
    if (string_list._0 == NULL) {
        printf("DEBUG: storeListProfiles returned empty list\n");
        return NULL;
    }
    
    // Get the count of strings in the list
    int32_t count = 0;
    ErrorCode count_result = askar_string_list_count(string_list, &count);
    if (count_result != 0 || count <= 0) {
        printf("DEBUG: storeListProfiles - no profiles found or error getting count\n");
        askar_string_list_free(string_list);
        return NULL;
    }
    
    // Create Java string array
    jclass string_class = (*env)->FindClass(env, "java/lang/String");
    if (string_class == NULL) {
        askar_string_list_free(string_list);
        return NULL;
    }
    
    jobjectArray profile_array = (*env)->NewObjectArray(env, count, string_class, NULL);
    if (profile_array == NULL) {
        askar_string_list_free(string_list);
        return NULL;
    }
    
    // Fill the array with profile names
    for (int32_t i = 0; i < count; i++) {
        const char* profile_name = NULL;
        ErrorCode get_result = askar_string_list_get_item(string_list, i, &profile_name);
        if (get_result == 0 && profile_name != NULL) {
            jstring jprofile = (*env)->NewStringUTF(env, profile_name);
            if (jprofile != NULL) {
                (*env)->SetObjectArrayElement(env, profile_array, i, jprofile);
                (*env)->DeleteLocalRef(env, jprofile);
            }
        }
    }
    
    askar_string_list_free(string_list);
    printf("DEBUG: storeListProfiles completed successfully, found %d profiles\n", count);
    return profile_array;
}

// ========== Advanced Session Operations ==========

// Session insert key
JNIEXPORT void JNICALL Java_org_hyperledger_aries_askar_AskarNative_sessionInsertKey
  (JNIEnv *env, jclass cls, jlong sessionHandle, jlong keyHandle, jstring name, jstring metadata, jstring tags, jlong expiryMs) {
    printf("DEBUG: sessionInsertKey called with sessionHandle=%ld, keyHandle=%ld\n", sessionHandle, keyHandle);
    
    const char* name_str = get_string_utf(env, name);
    const char* metadata_str = get_string_utf(env, metadata);
    const char* tags_str = get_string_utf(env, tags);
    
    if (name_str == NULL) {
        return;
    }
    
    SessionHandle session = (SessionHandle)sessionHandle;
    LocalKeyHandle key = (LocalKeyHandle){(void*)(uintptr_t)keyHandle};
    
    ErrorCode result = askar_session_insert_key(session, key, name_str, metadata_str, tags_str, 
                                               (int64_t)expiryMs, void_callback, 1);
    
    if (result == 0) {
        result = wait_for_callback();
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: sessionInsertKey failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        // Release strings before throwing
        release_string_utf(env, name, name_str);
        release_string_utf(env, metadata, metadata_str);
        release_string_utf(env, tags, tags_str);
        
        jclass exception_class = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exception_class) {
            char error_msg[256];
            snprintf(error_msg, sizeof(error_msg), "Session insert key failed with error code: %ld", result);
            (*env)->ThrowNew(env, exception_class, error_msg);
        }
        return;
    }
    
    release_string_utf(env, name, name_str);
    release_string_utf(env, metadata, metadata_str);
    release_string_utf(env, tags, tags_str);
    printf("DEBUG: sessionInsertKey completed successfully\n");
}

// Callback for key entry list operations
void key_entry_list_callback(int64_t callback_id, ErrorCode error_code, KeyEntryListHandle key_entry_list_handle) {
    pthread_mutex_lock(&callback_mutex);
    callback_result = (int64_t)(uintptr_t)key_entry_list_handle._0;
    callback_error = error_code;
    callback_completed = 1;
    pthread_cond_signal(&callback_cond);
    pthread_mutex_unlock(&callback_mutex);
}

// Session fetch key
JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_sessionFetchKey
  (JNIEnv *env, jclass cls, jlong sessionHandle, jstring name, jboolean forUpdate) {
    printf("DEBUG: sessionFetchKey called with sessionHandle=%ld, forUpdate=%s\n", sessionHandle, forUpdate ? "true" : "false");
    
    const char* name_str = get_string_utf(env, name);
    if (name_str == NULL) {
        return 0;
    }
    
    SessionHandle session = (SessionHandle)sessionHandle;
    int8_t for_update_flag = forUpdate ? 1 : 0;
    
    ErrorCode result = askar_session_fetch_key(session, name_str, for_update_flag, key_entry_list_callback, 1);
    
    if (result == 0) {
        result = wait_for_callback();
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: sessionFetchKey failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        release_string_utf(env, name, name_str);
        return 0;
    }
    
    jlong key_entry_list_handle = (jlong)callback_result;
    release_string_utf(env, name, name_str);
    printf("DEBUG: sessionFetchKey completed successfully, handle=%ld\n", key_entry_list_handle);
    return key_entry_list_handle;
}

// Session update key
JNIEXPORT void JNICALL Java_org_hyperledger_aries_askar_AskarNative_sessionUpdateKey
  (JNIEnv *env, jclass cls, jlong sessionHandle, jstring name, jstring metadata, jstring tags, jlong expiryMs) {
    printf("DEBUG: sessionUpdateKey called with sessionHandle=%ld\n", sessionHandle);
    
    const char* name_str = get_string_utf(env, name);
    const char* metadata_str = get_string_utf(env, metadata);
    const char* tags_str = get_string_utf(env, tags);
    
    if (name_str == NULL) {
        return;
    }
    
    SessionHandle session = (SessionHandle)sessionHandle;
    
    ErrorCode result = askar_session_update_key(session, name_str, metadata_str, tags_str, 
                                               (int64_t)expiryMs, void_callback, 1);
    
    if (result == 0) {
        result = wait_for_callback();
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: sessionUpdateKey failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        // Release strings before throwing
        release_string_utf(env, name, name_str);
        release_string_utf(env, metadata, metadata_str);
        release_string_utf(env, tags, tags_str);
        
        jclass exception_class = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exception_class) {
            char error_msg[256];
            snprintf(error_msg, sizeof(error_msg), "Session update key failed with error code: %ld", result);
            (*env)->ThrowNew(env, exception_class, error_msg);
        }
        return;
    }
    
    release_string_utf(env, name, name_str);
    release_string_utf(env, metadata, metadata_str);
    release_string_utf(env, tags, tags_str);
    printf("DEBUG: sessionUpdateKey completed successfully\n");
}

// Session remove key
JNIEXPORT void JNICALL Java_org_hyperledger_aries_askar_AskarNative_sessionRemoveKey
  (JNIEnv *env, jclass cls, jlong sessionHandle, jstring name) {
    printf("DEBUG: sessionRemoveKey called with sessionHandle=%ld\n", sessionHandle);
    
    const char* name_str = get_string_utf(env, name);
    if (name_str == NULL) {
        return;
    }
    
    SessionHandle session = (SessionHandle)sessionHandle;
    
    ErrorCode result = askar_session_remove_key(session, name_str, void_callback, 1);
    
    if (result == 0) {
        result = wait_for_callback();
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: sessionRemoveKey failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        release_string_utf(env, name, name_str);
        
        jclass exception_class = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exception_class) {
            char error_msg[256];
            snprintf(error_msg, sizeof(error_msg), "Session remove key failed with error code: %ld", result);
            (*env)->ThrowNew(env, exception_class, error_msg);
        }
        return;
    }
    
    release_string_utf(env, name, name_str);
    printf("DEBUG: sessionRemoveKey completed successfully\n");
}

// Session fetch all keys
JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_sessionFetchAllKeys
  (JNIEnv *env, jclass cls, jlong sessionHandle, jstring tagFilter, jint limit, jboolean forUpdate) {
    printf("DEBUG: sessionFetchAllKeys called with sessionHandle=%ld, limit=%d, forUpdate=%s\n", 
           sessionHandle, limit, forUpdate ? "true" : "false");
    
    const char* tag_filter_str = get_string_utf(env, tagFilter);
    
    SessionHandle session = (SessionHandle)sessionHandle;
    int8_t for_update_flag = forUpdate ? 1 : 0;
    
    ErrorCode result = askar_session_fetch_all_keys(session, NULL, NULL, tag_filter_str, (int64_t)limit, 
                                                   for_update_flag, key_entry_list_callback, 1);
    
    if (result == 0) {
        result = wait_for_callback();
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: sessionFetchAllKeys failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        release_string_utf(env, tagFilter, tag_filter_str);
        return 0;
    }
    
    jlong key_entry_list_handle = (jlong)callback_result;
    release_string_utf(env, tagFilter, tag_filter_str);
    printf("DEBUG: sessionFetchAllKeys completed successfully, handle=%ld\n", key_entry_list_handle);
    return key_entry_list_handle;
}

// Session remove all
JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_sessionRemoveAll
  (JNIEnv *env, jclass cls, jlong sessionHandle, jstring category, jstring tagFilter) {
    printf("DEBUG: sessionRemoveAll called with sessionHandle=%ld\n", sessionHandle);
    
    const char* category_str = get_string_utf(env, category);
    const char* tag_filter_str = get_string_utf(env, tagFilter);
    
    SessionHandle session = (SessionHandle)sessionHandle;
    
    ErrorCode result = askar_session_remove_all(session, category_str, tag_filter_str, count_callback, 1);
    
    if (result == 0) {
        result = wait_for_callback();
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: sessionRemoveAll failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        release_string_utf(env, category, category_str);
        release_string_utf(env, tagFilter, tag_filter_str);
        return 0;
    }
    
    jlong removed_count = (jlong)callback_result;
    release_string_utf(env, category, category_str);
    release_string_utf(env, tagFilter, tag_filter_str);
    printf("DEBUG: sessionRemoveAll completed successfully, removed count=%ld\n", removed_count);
    return removed_count;
}

// ========== Scan Operations ==========

// Callback for scan handle operations
void scan_handle_callback(int64_t callback_id, ErrorCode error_code, ScanHandle scan_handle) {
    pthread_mutex_lock(&callback_mutex);
    callback_result = (int64_t)scan_handle;
    callback_error = error_code;
    callback_completed = 1;
    pthread_cond_signal(&callback_cond);
    pthread_mutex_unlock(&callback_mutex);
}

// Scan start
JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_scanStart
  (JNIEnv *env, jclass cls, jlong storeHandle, jstring profile, jstring category, jstring tagFilter, 
   jlong offset, jlong limit, jstring orderBy, jboolean descending) {
    printf("DEBUG: scanStart called with storeHandle=%ld, offset=%ld, limit=%ld, descending=%s\n", 
           storeHandle, offset, limit, descending ? "true" : "false");
    
    const char* profile_str = get_string_utf(env, profile);
    const char* category_str = get_string_utf(env, category);
    const char* tag_filter_str = get_string_utf(env, tagFilter);
    const char* order_by_str = get_string_utf(env, orderBy);
    
    StoreHandle store = (StoreHandle)storeHandle;
    int8_t descending_flag = descending ? 1 : 0;
    
    ErrorCode result = askar_scan_start(store, profile_str, category_str, tag_filter_str, 
                                       (int64_t)offset, (int64_t)limit, order_by_str, 
                                       descending_flag, scan_handle_callback, 1);
    
    if (result == 0) {
        result = wait_for_callback();
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: scanStart failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        
        release_string_utf(env, profile, profile_str);
        release_string_utf(env, category, category_str);
        release_string_utf(env, tagFilter, tag_filter_str);
        release_string_utf(env, orderBy, order_by_str);
        return 0;
    }
    
    jlong scan_handle = (jlong)callback_result;
    release_string_utf(env, profile, profile_str);
    release_string_utf(env, category, category_str);
    release_string_utf(env, tagFilter, tag_filter_str);
    release_string_utf(env, orderBy, order_by_str);
    printf("DEBUG: scanStart completed successfully, scan handle=%ld\n", scan_handle);
    return scan_handle;
}

// Scan next
JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_scanNext
  (JNIEnv *env, jclass cls, jlong scanHandle) {
    printf("DEBUG: scanNext called with scanHandle=%ld\n", scanHandle);
    
    ScanHandle scan = (ScanHandle)scanHandle;
    
    ErrorCode result = askar_scan_next(scan, entry_list_callback, 1);
    
    if (result == 0) {
        result = wait_for_callback();
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: scanNext failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        return 0;
    }
    
    jlong entry_list_handle = (jlong)callback_result;
    printf("DEBUG: scanNext completed successfully, entry list handle=%ld\n", entry_list_handle);
    return entry_list_handle;
}

// Scan free
JNIEXPORT void JNICALL Java_org_hyperledger_aries_askar_AskarNative_scanFree
  (JNIEnv *env, jclass cls, jlong scanHandle) {
    printf("DEBUG: scanFree called with scanHandle=%ld\n", scanHandle);
    
    ScanHandle scan = (ScanHandle)scanHandle;
    
    ErrorCode result = askar_scan_free(scan);
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: scanFree failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        return;
    }
    
    printf("DEBUG: scanFree completed successfully\n");
}

// ========== Key Entry List Operations ==========

// Key entry list count
JNIEXPORT jint JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyEntryListCount
  (JNIEnv *env, jclass cls, jlong keyEntryListHandle) {
    printf("DEBUG: keyEntryListCount called with handle=%ld\n", keyEntryListHandle);
    
    KeyEntryListHandle key_entry_list;
    key_entry_list._0 = (FfiKeyEntryList*)(uintptr_t)keyEntryListHandle;
    
    if (key_entry_list._0 == NULL) {
        printf("DEBUG: keyEntryListCount - invalid handle\n");
        return 0;
    }
    
    int32_t count = 0;
    ErrorCode result = askar_key_entry_list_count(key_entry_list, &count);
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: keyEntryListCount failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        return 0;
    }
    
    printf("DEBUG: keyEntryListCount completed successfully, count=%d\n", count);
    return (jint)count;
}

// Key entry list get algorithm
JNIEXPORT jstring JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyEntryListGetAlgorithm
  (JNIEnv *env, jclass cls, jlong keyEntryListHandle, jint index) {
    printf("DEBUG: keyEntryListGetAlgorithm called with handle=%ld, index=%d\n", keyEntryListHandle, index);
    
    KeyEntryListHandle key_entry_list;
    key_entry_list._0 = (FfiKeyEntryList*)(uintptr_t)keyEntryListHandle;
    
    if (key_entry_list._0 == NULL) {
        printf("DEBUG: keyEntryListGetAlgorithm - invalid handle\n");
        return NULL;
    }
    
    const char* algorithm = NULL;
    ErrorCode result = askar_key_entry_list_get_algorithm(key_entry_list, (int32_t)index, &algorithm);
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: keyEntryListGetAlgorithm failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        return NULL;
    }
    
    jstring jalgorithm = NULL;
    if (algorithm) {
        jalgorithm = (*env)->NewStringUTF(env, algorithm);
    }
    
    printf("DEBUG: keyEntryListGetAlgorithm completed successfully\n");
    return jalgorithm;
}

// Key entry list get name
JNIEXPORT jstring JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyEntryListGetName
  (JNIEnv *env, jclass cls, jlong keyEntryListHandle, jint index) {
    printf("DEBUG: keyEntryListGetName called with handle=%ld, index=%d\n", keyEntryListHandle, index);
    
    KeyEntryListHandle key_entry_list;
    key_entry_list._0 = (FfiKeyEntryList*)(uintptr_t)keyEntryListHandle;
    
    if (key_entry_list._0 == NULL) {
        printf("DEBUG: keyEntryListGetName - invalid handle\n");
        return NULL;
    }
    
    const char* name = NULL;
    ErrorCode result = askar_key_entry_list_get_name(key_entry_list, (int32_t)index, &name);
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: keyEntryListGetName failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        return NULL;
    }
    
    jstring jname = NULL;
    if (name) {
        jname = (*env)->NewStringUTF(env, name);
    }
    
    printf("DEBUG: keyEntryListGetName completed successfully\n");
    return jname;
}

// Key entry list get metadata
JNIEXPORT jstring JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyEntryListGetMetadata
  (JNIEnv *env, jclass cls, jlong keyEntryListHandle, jint index) {
    printf("DEBUG: keyEntryListGetMetadata called with handle=%ld, index=%d\n", keyEntryListHandle, index);
    
    KeyEntryListHandle key_entry_list;
    key_entry_list._0 = (FfiKeyEntryList*)(uintptr_t)keyEntryListHandle;
    
    if (key_entry_list._0 == NULL) {
        printf("DEBUG: keyEntryListGetMetadata - invalid handle\n");
        return NULL;
    }
    
    const char* metadata = NULL;
    ErrorCode result = askar_key_entry_list_get_metadata(key_entry_list, (int32_t)index, &metadata);
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: keyEntryListGetMetadata failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        return NULL;
    }
    
    jstring jmetadata = NULL;
    if (metadata) {
        jmetadata = (*env)->NewStringUTF(env, metadata);
    }
    
    printf("DEBUG: keyEntryListGetMetadata completed successfully\n");
    return jmetadata;
}

// Key entry list get tags
JNIEXPORT jstring JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyEntryListGetTags
  (JNIEnv *env, jclass cls, jlong keyEntryListHandle, jint index) {
    printf("DEBUG: keyEntryListGetTags called with handle=%ld, index=%d\n", keyEntryListHandle, index);
    
    KeyEntryListHandle key_entry_list;
    key_entry_list._0 = (FfiKeyEntryList*)(uintptr_t)keyEntryListHandle;
    
    if (key_entry_list._0 == NULL) {
        printf("DEBUG: keyEntryListGetTags - invalid handle\n");
        return NULL;
    }
    
    const char* tags = NULL;
    ErrorCode result = askar_key_entry_list_get_tags(key_entry_list, (int32_t)index, &tags);
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: keyEntryListGetTags failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        return NULL;
    }
    
    jstring jtags = NULL;
    if (tags) {
        jtags = (*env)->NewStringUTF(env, tags);
    }
    
    printf("DEBUG: keyEntryListGetTags completed successfully\n");
    return jtags;
}

// Key entry list load key
JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyEntryListLoadKey
  (JNIEnv *env, jclass cls, jlong keyEntryListHandle, jint index) {
    printf("DEBUG: keyEntryListLoadKey called with handle=%ld, index=%d\n", keyEntryListHandle, index);
    
    KeyEntryListHandle key_entry_list;
    key_entry_list._0 = (FfiKeyEntryList*)(uintptr_t)keyEntryListHandle;
    
    if (key_entry_list._0 == NULL) {
        printf("DEBUG: keyEntryListLoadKey - invalid handle\n");
        return 0;
    }
    
    LocalKeyHandle key_handle;
    ErrorCode result = askar_key_entry_list_load_local(key_entry_list, (int32_t)index, &key_handle);
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: keyEntryListLoadKey failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        return 0;
    }
    
    jlong jkey_handle = (jlong)(uintptr_t)key_handle._0;
    printf("DEBUG: keyEntryListLoadKey completed successfully, key handle=%ld\n", jkey_handle);
    return jkey_handle;
}

// Key entry list free
JNIEXPORT void JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyEntryListFree
  (JNIEnv *env, jclass cls, jlong keyEntryListHandle) {
    printf("DEBUG: keyEntryListFree called with handle=%ld\n", keyEntryListHandle);
    
    KeyEntryListHandle key_entry_list;
    key_entry_list._0 = (FfiKeyEntryList*)(uintptr_t)keyEntryListHandle;
    
    if (key_entry_list._0 == NULL) {
        printf("DEBUG: keyEntryListFree - invalid handle\n");
        return;
    }
    
    askar_key_entry_list_free(key_entry_list);
    printf("DEBUG: keyEntryListFree completed successfully\n");
}

// ========== Additional Utility Functions ==========

// Store generate raw key
JNIEXPORT jstring JNICALL Java_org_hyperledger_aries_askar_AskarNative_storeGenerateRawKey
  (JNIEnv *env, jclass cls, jbyteArray seed) {
    printf("DEBUG: storeGenerateRawKey called\n");
    
    struct ByteBuffer seed_buffer = {0};
    
    if (seed != NULL) {
        jsize seed_len = (*env)->GetArrayLength(env, seed);
        jbyte* seed_bytes = (*env)->GetByteArrayElements(env, seed, NULL);
        
        seed_buffer.len = (int64_t)seed_len;
        seed_buffer.data = (uint8_t*)seed_bytes;
    }
    
    const char* raw_key = NULL;
    ErrorCode result = askar_store_generate_raw_key(seed_buffer, &raw_key);
    
    if (seed != NULL) {
        jbyte* seed_bytes = (jbyte*)seed_buffer.data;
        (*env)->ReleaseByteArrayElements(env, seed, seed_bytes, JNI_ABORT);
    }
    
    if (result != 0) {
        const char* error_json = NULL;
        askar_get_current_error(&error_json);
        
        printf("DEBUG: storeGenerateRawKey failed with error: %ld\n", result);
        if (error_json) {
            printf("DEBUG: Error details: %s\n", error_json);
        }
        return NULL;
    }
    
    jstring jraw_key = NULL;
    if (raw_key) {
        jraw_key = (*env)->NewStringUTF(env, raw_key);
    }
    
    printf("DEBUG: storeGenerateRawKey completed successfully\n");
    return jraw_key;
}