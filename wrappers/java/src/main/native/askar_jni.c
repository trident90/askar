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
void store_handle_callback(int64_t callback_id, ErrorCode error_code, size_t store_handle) {
    pthread_mutex_lock(&callback_mutex);
    callback_result = (int64_t)store_handle;
    callback_error = error_code;
    callback_completed = 1;
    pthread_cond_signal(&callback_cond);
    pthread_mutex_unlock(&callback_mutex);
}

// Callback for session_start
void session_handle_callback(int64_t callback_id, ErrorCode error_code, size_t session_handle) {
    pthread_mutex_lock(&callback_mutex);
    callback_result = (int64_t)session_handle;
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

// Callback for functions that return counts (session_count)
void count_callback(int64_t callback_id, ErrorCode error_code, int64_t count) {
    pthread_mutex_lock(&callback_mutex);
    callback_result = count;
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

// Library management functions
JNIEXPORT jstring JNICALL Java_org_hyperledger_aries_askar_AskarNative_getVersion
  (JNIEnv *env, jclass cls) {
    char* version = askar_version();
    if (version == NULL) return NULL;
    
    jstring result = (*env)->NewStringUTF(env, version);
    // Note: askar_version() returns a static string, no need to free it
    return result;
}

JNIEXPORT void JNICALL Java_org_hyperledger_aries_askar_AskarNative_freeString
  (JNIEnv *env, jclass cls, jlong stringPtr) {
    if (stringPtr != 0) {
        askar_string_free((char*)stringPtr);
    }
}

JNIEXPORT jint JNICALL Java_org_hyperledger_aries_askar_AskarNative_setMaxLogLevel
  (JNIEnv *env, jclass cls, jint level) {
    return (jint)askar_set_max_log_level((int32_t)level);
}

JNIEXPORT void JNICALL Java_org_hyperledger_aries_askar_AskarNative_terminate
  (JNIEnv *env, jclass cls) {
    askar_terminate();
}

// Store operations
JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_storeProvision
  (JNIEnv *env, jclass cls, jstring uri, jstring keyMethod, jstring passKey, jstring profile, jboolean recreate) {
    
    const char* c_uri = get_string_utf(env, uri);
    const char* c_key_method = get_string_utf(env, keyMethod);
    const char* c_pass_key = get_string_utf(env, passKey);
    const char* c_profile = get_string_utf(env, profile);
    
    // Call Rust function with callback
    ErrorCode result = askar_store_provision(
        c_uri, c_key_method, c_pass_key, c_profile, 
        (int8_t)recreate, store_handle_callback, 1
    );
    
    // Release strings
    release_string_utf(env, uri, c_uri);
    release_string_utf(env, keyMethod, c_key_method);
    release_string_utf(env, passKey, c_pass_key);
    release_string_utf(env, profile, c_profile);
    
    if (result != 0) {
        // Throw Java exception
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[256];
            snprintf(error_msg, sizeof(error_msg), "Store provision failed with error code: %d", result);
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return 0;
    }
    
    // Wait for callback and return the store handle
    ErrorCode callback_err = wait_for_callback();
    if (callback_err != 0) {
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[256];
            snprintf(error_msg, sizeof(error_msg), "Store provision callback failed with error code: %d", callback_err);
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return 0;
    }
    
    return (jlong)callback_result;
}

JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_storeOpen
  (JNIEnv *env, jclass cls, jstring uri, jstring keyMethod, jstring passKey, jstring profile) {
    
    const char* c_uri = get_string_utf(env, uri);
    const char* c_key_method = get_string_utf(env, keyMethod);
    const char* c_pass_key = get_string_utf(env, passKey);
    const char* c_profile = get_string_utf(env, profile);
    
    ErrorCode result = askar_store_open(
        c_uri, c_key_method, c_pass_key, c_profile, 
        store_handle_callback, 2
    );
    
    release_string_utf(env, uri, c_uri);
    release_string_utf(env, keyMethod, c_key_method);
    release_string_utf(env, passKey, c_pass_key);
    release_string_utf(env, profile, c_profile);
    
    if (result != 0) {
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[256];
            snprintf(error_msg, sizeof(error_msg), "Store open failed with error code: %d", result);
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return 0;
    }
    
    ErrorCode callback_err = wait_for_callback();
    if (callback_err != 0) {
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[256];
            snprintf(error_msg, sizeof(error_msg), "Store open callback failed with error code: %d", callback_err);
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return 0;
    }
    
    return (jlong)callback_result;
}

JNIEXPORT void JNICALL Java_org_hyperledger_aries_askar_AskarNative_storeClose
  (JNIEnv *env, jclass cls, jlong storeHandle) {
    
    ErrorCode result = askar_store_close((StoreHandle)storeHandle, void_callback, 3);
    
    if (result != 0) {
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[256];
            snprintf(error_msg, sizeof(error_msg), "Store close failed with error code: %d", result);
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return;
    }
    
    wait_for_callback(); // Wait for completion but ignore result for close operation
}

// Session operations
JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_sessionStart
  (JNIEnv *env, jclass cls, jlong storeHandle, jstring profile, jboolean asTransaction) {
    
    const char* c_profile = get_string_utf(env, profile);
    
    ErrorCode result = askar_session_start(
        (StoreHandle)storeHandle, c_profile, 
        (int8_t)asTransaction, session_handle_callback, 4
    );
    
    release_string_utf(env, profile, c_profile);
    
    if (result != 0) {
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[256];
            snprintf(error_msg, sizeof(error_msg), "Session start failed with error code: %d", result);
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return 0;
    }
    
    ErrorCode callback_err = wait_for_callback();
    if (callback_err != 0) {
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[256];
            snprintf(error_msg, sizeof(error_msg), "Session start callback failed with error code: %d", callback_err);
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return 0;
    }
    
    return (jlong)callback_result;
}

JNIEXPORT jint JNICALL Java_org_hyperledger_aries_askar_AskarNative_sessionCount
  (JNIEnv *env, jclass cls, jlong sessionHandle, jstring category, jstring tagFilter) {
    
    const char* c_category = get_string_utf(env, category);
    const char* c_tag_filter = get_string_utf(env, tagFilter);
    
    ErrorCode result = askar_session_count(
        (SessionHandle)sessionHandle, c_category, c_tag_filter,
        count_callback, 5
    );
    
    release_string_utf(env, category, c_category);
    release_string_utf(env, tagFilter, c_tag_filter);
    
    if (result != 0) {
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[256];
            snprintf(error_msg, sizeof(error_msg), "Session count failed with error code: %d", result);
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return -1;
    }
    
    ErrorCode callback_err = wait_for_callback();
    if (callback_err != 0) {
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[256];
            snprintf(error_msg, sizeof(error_msg), "Session count callback failed with error code: %d", callback_err);
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return -1;
    }
    
    return (jint)callback_result;
}

// Entry list operations
JNIEXPORT jint JNICALL Java_org_hyperledger_aries_askar_AskarNative_entryListCount
  (JNIEnv *env, jclass cls, jlong entryListHandle) {
    // TODO: Implement EntryListHandle count
    return 0;
}

JNIEXPORT jstring JNICALL Java_org_hyperledger_aries_askar_AskarNative_entryListGetCategory
  (JNIEnv *env, jclass cls, jlong entryListHandle, jint index) {
    
    char* category = NULL;
    ErrorCode result = askar_entry_list_get_category(
        (EntryListHandle)entryListHandle, (int32_t)index, &category
    );
    
    if (result != 0 || category == NULL) {
        return NULL;
    }
    
    jstring jcategory = (*env)->NewStringUTF(env, category);
    askar_string_free(category);
    return jcategory;
}

JNIEXPORT jstring JNICALL Java_org_hyperledger_aries_askar_AskarNative_entryListGetName
  (JNIEnv *env, jclass cls, jlong entryListHandle, jint index) {
    
    char* name = NULL;
    ErrorCode result = askar_entry_list_get_name(
        (EntryListHandle)entryListHandle, (int32_t)index, &name
    );
    
    if (result != 0 || name == NULL) {
        return NULL;
    }
    
    jstring jname = (*env)->NewStringUTF(env, name);
    askar_string_free(name);
    return jname;
}

JNIEXPORT jbyteArray JNICALL Java_org_hyperledger_aries_askar_AskarNative_entryListGetValue
  (JNIEnv *env, jclass cls, jlong entryListHandle, jint index) {
    
    SecretBuffer buffer = {0};
    ErrorCode result = askar_entry_list_get_value(
        (EntryListHandle)entryListHandle, (int32_t)index, &buffer
    );
    
    if (result != 0 || buffer.data == NULL || buffer.len <= 0) {
        return NULL;
    }
    
    // Create Java byte array
    jbyteArray jarray = (*env)->NewByteArray(env, (jsize)buffer.len);
    if (jarray == NULL) {
        askar_buffer_free(buffer);
        return NULL;
    }
    
    // Copy data to Java array
    (*env)->SetByteArrayRegion(env, jarray, 0, (jsize)buffer.len, (jbyte*)buffer.data);
    
    // Free Rust memory
    askar_buffer_free(buffer);
    
    return jarray;
}

JNIEXPORT jstring JNICALL Java_org_hyperledger_aries_askar_AskarNative_entryListGetTags
  (JNIEnv *env, jclass cls, jlong entryListHandle, jint index) {
    
    char* tags = NULL;
    ErrorCode result = askar_entry_list_get_tags(
        (EntryListHandle)entryListHandle, (int32_t)index, &tags
    );
    
    if (result != 0 || tags == NULL) {
        return NULL;
    }
    
    jstring jtags = (*env)->NewStringUTF(env, tags);
    askar_string_free(tags);
    return jtags;
}

JNIEXPORT void JNICALL Java_org_hyperledger_aries_askar_AskarNative_entryListFree
  (JNIEnv *env, jclass cls, jlong entryListHandle) {
    // TODO: Implement EntryListHandle management
    // EntryListHandle is a struct, need proper conversion
}

// Session fetch implementation
JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_sessionFetch
  (JNIEnv *env, jclass cls, jlong sessionHandle, jstring category, jstring name, jboolean forUpdate) {
    
    const char* c_category = get_string_utf(env, category);
    const char* c_name = get_string_utf(env, name);
    
    ErrorCode result = askar_session_fetch(
        (SessionHandle)sessionHandle, c_category, c_name, (int8_t)forUpdate,
        entry_list_callback, 9
    );
    
    release_string_utf(env, category, c_category);
    release_string_utf(env, name, c_name);
    
    if (result != 0) {
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[256];
            snprintf(error_msg, sizeof(error_msg), "Session fetch failed with error code: %d", result);
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return 0;
    }
    
    ErrorCode callback_err = wait_for_callback();
    if (callback_err != 0) {
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[256];
            snprintf(error_msg, sizeof(error_msg), "Session fetch callback failed with error code: %d", callback_err);
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return 0;
    }
    
    return (jlong)callback_result;
}

JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_sessionFetchAll
  (JNIEnv *env, jclass cls, jlong sessionHandle, jstring category, jstring tagFilter, 
   jint limit, jstring orderBy, jboolean descending, jboolean forUpdate) {
    
    const char* c_category = get_string_utf(env, category);
    const char* c_tag_filter = get_string_utf(env, tagFilter);
    const char* c_order_by = get_string_utf(env, orderBy);
    
    ErrorCode result = askar_session_fetch_all(
        (SessionHandle)sessionHandle, c_category, c_tag_filter,
        (int32_t)limit, c_order_by, (int8_t)descending, (int8_t)forUpdate,
        entry_list_callback, 6
    );
    
    release_string_utf(env, category, c_category);
    release_string_utf(env, tagFilter, c_tag_filter);
    release_string_utf(env, orderBy, c_order_by);
    
    if (result != 0) {
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[256];
            snprintf(error_msg, sizeof(error_msg), "Session fetch all failed with error code: %d", result);
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return 0;
    }
    
    ErrorCode callback_err = wait_for_callback();
    if (callback_err != 0) {
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[256];
            snprintf(error_msg, sizeof(error_msg), "Session fetch all callback failed with error code: %d", callback_err);
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return 0;
    }
    
    return (jlong)callback_result;
}

JNIEXPORT void JNICALL Java_org_hyperledger_aries_askar_AskarNative_sessionUpdate
  (JNIEnv *env, jclass cls, jlong sessionHandle, jbyte operation, jstring category, 
   jstring name, jbyteArray value, jstring tags, jlong expiryMs) {
    
    // Validate inputs
    if (sessionHandle == 0) {
        jclass exc = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (exc != NULL) {
            (*env)->ThrowNew(env, exc, "Invalid session handle");
        }
        return;
    }
    
    if (operation < 0 || operation > 2) {
        jclass exc = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (exc != NULL) {
            (*env)->ThrowNew(env, exc, "Invalid operation: must be 0 (Insert), 1 (Replace), or 2 (Remove)");
        }
        return;
    }
    
    const char* c_category = get_string_utf(env, category);
    const char* c_name = get_string_utf(env, name);
    const char* c_tags = get_string_utf(env, tags);
    
    // Validate required parameters
    if (c_category == NULL) {
        jclass exc = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (exc != NULL) {
            (*env)->ThrowNew(env, exc, "Category cannot be null");
        }
        goto cleanup;
    }
    
    if (c_name == NULL) {
        jclass exc = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (exc != NULL) {
            (*env)->ThrowNew(env, exc, "Name cannot be null");
        }
        goto cleanup;
    }
    
    // Convert byte array to ByteBuffer
    ByteBuffer buffer = {0};
    jbyte* value_data = NULL;
    if (value != NULL) {
        jsize len = (*env)->GetArrayLength(env, value);
        buffer.len = (int64_t)len;
        if (len > 0) {
            // Get a direct pointer to the Java byte array data
            value_data = (*env)->GetByteArrayElements(env, value, NULL);
            if (value_data == NULL) {
                jclass exc = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
                if (exc != NULL) {
                    (*env)->ThrowNew(env, exc, "Failed to access byte array data");
                }
                goto cleanup;
            }
            buffer.data = (uint8_t*)value_data;
        } else {
            buffer.data = NULL;
        }
    } else {
        // For Remove operations, value can be NULL
        buffer.len = 0;
        buffer.data = NULL;
    }
    
    // Debug information
    printf("DEBUG: sessionUpdate called with handle=%ld (0x%lx), operation=%d, category=%s, name=%s, value_len=%ld, tags=%s, expiry=%ld\n",
            sessionHandle, sessionHandle, operation, c_category ? c_category : "NULL", c_name ? c_name : "NULL", 
            buffer.len, c_tags ? c_tags : "NULL", expiryMs);
    printf("DEBUG: SessionHandle cast check: size_t=%zu, jlong=%zu\n", sizeof(size_t), sizeof(jlong));
    fflush(stdout);
    
    ErrorCode result = askar_session_update(
        (SessionHandle)sessionHandle, (int8_t)operation, c_category, c_name,
        buffer, c_tags, expiryMs >= 0 ? expiryMs : -1,
        void_callback, 8
    );
    
    if (result != 0) {
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[256];
            snprintf(error_msg, sizeof(error_msg), "Session update failed with error code: %d", result);
            (*env)->ThrowNew(env, exc, error_msg);
        }
        goto cleanup;
    }
    
    ErrorCode callback_err = wait_for_callback();
    if (callback_err != 0) {
        // Get detailed error message from Rust
        char* error_json = NULL;
        askar_get_current_error((const char**)&error_json);
        
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[512];
            if (error_json) {
                snprintf(error_msg, sizeof(error_msg), "Session update callback failed with error code: %ld, detail: %s", callback_err, error_json);
                askar_string_free(error_json);
            } else {
                snprintf(error_msg, sizeof(error_msg), "Session update callback failed with error code: %ld", callback_err);
            }
            (*env)->ThrowNew(env, exc, error_msg);
        }
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
        jclass exc = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exc != NULL) {
            char error_msg[256];
            snprintf(error_msg, sizeof(error_msg), "Session close failed with error code: %d", result);
            (*env)->ThrowNew(env, exc, error_msg);
        }
        return;
    }
    
    wait_for_callback(); // Wait for completion
}

JNIEXPORT jlong JNICALL Java_org_hyperledger_aries_askar_AskarNative_keyGenerate
  (JNIEnv *env, jclass cls, jstring algorithm, jstring backend, jboolean ephemeral) {
    
    const char* c_algorithm = get_string_utf(env, algorithm);
    const char* c_key_backend = get_string_utf(env, backend);
    
    printf("DEBUG: keyGenerate called with algorithm=%s, keyBackend=%s, ephemeral=%d\n",
            c_algorithm ? c_algorithm : "NULL", c_key_backend ? c_key_backend : "NULL", ephemeral);
    fflush(stdout);
    
    LocalKeyHandle key_handle = {NULL};
    ErrorCode result = askar_key_generate(
        c_algorithm, c_key_backend, (int8_t)ephemeral, &key_handle
    );
    
    release_string_utf(env, algorithm, c_algorithm);
    release_string_utf(env, backend, c_key_backend);
    
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

JNIEXPORT jstring JNICALL Java_org_hyperledger_aries_askar_AskarNative_getLastError
  (JNIEnv *env, jclass cls) {
    char* error_json = NULL;
    ErrorCode result = askar_get_current_error((const char**)&error_json);
    
    if (result != 0 || error_json == NULL) {
        return NULL;
    }
    
    jstring jerror = (*env)->NewStringUTF(env, error_json);
    askar_string_free(error_json);
    return jerror;
}

