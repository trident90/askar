package org.hyperledger.aries.askar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages the native library loading and provides utility functions for FFI operations.
 */
public class LibraryLoader {

    private static final Logger logger = LoggerFactory.getLogger(LibraryLoader.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final AtomicLong callbackCounter = new AtomicLong(1);
    private static final ConcurrentHashMap<Long, CompletableFuture<Pointer>> pendingCallbacks = new ConcurrentHashMap<>();

    private static volatile boolean initialized = false;
    private static final Object initLock = new Object();

    /**
     * Initialize the library if not already done.
     */
    public static void ensureInitialized() {
        if (!initialized) {
            synchronized (initLock) {
                if (!initialized) {
                    try {
                        AskarLibrary.INSTANCE.askar_version();
                        initialized = true;
                        logger.info("Askar library initialized successfully");
                    } catch (UnsatisfiedLinkError e) {
                        throw new RuntimeException("Failed to load Askar native library", e);
                    }
                }
            }
        }
    }

    /**
     * Get the version of the loaded library.
     * @return Library version string
     */
    public static String getVersion() {
        ensureInitialized();
        Pointer versionPtr = AskarLibrary.INSTANCE.askar_version();
        try {
            return versionPtr.getString(0);
        } finally {
            AskarLibrary.INSTANCE.askar_string_free(versionPtr);
        }
    }

    /**
     * Check for library errors and throw exception if found.
     * @param errorCode The error code returned by a library function
     * @throws AskarException If an error occurred
     */
    public static void checkError(int errorCode) throws AskarException {
        if (errorCode == 0) {
            return;
        }

        PointerByReference errorJsonRef = new PointerByReference();
        int result = AskarLibrary.INSTANCE.askar_get_current_error(errorJsonRef);

        if (result == 0 && errorJsonRef.getValue() != null && errorJsonRef.getValue() != Pointer.NULL) {
            try {
                String errorJson = errorJsonRef.getValue().getString(0);
                JsonNode errorNode = objectMapper.readTree(errorJson);

                int code = errorNode.has("code") ? errorNode.get("code").asInt() : errorCode;
                String message = errorNode.has("message") ? errorNode.get("message").asText() : "Unknown error";
                Object extra = errorNode.has("extra") ? errorNode.get("extra") : null;

                throw new AskarException(AskarException.ErrorCode.fromCode(code), message, extra);
            } catch (JsonProcessingException e) {
                logger.warn("Failed to parse error JSON", e);
            } finally {
                AskarLibrary.INSTANCE.askar_string_free(errorJsonRef.getValue());
            }
        }

        throw new AskarException(AskarException.ErrorCode.fromCode(errorCode), "Unknown error occurred");
    }

    /**
     * Create a callback for async operations.
     * @return A CompletableFuture that will complete when the operation finishes
     */
    public static CompletableFuture<Pointer> createCallback() {
        long callbackId = callbackCounter.getAndIncrement();
        CompletableFuture<Pointer> future = new CompletableFuture<>();
        pendingCallbacks.put(callbackId, future);
        return future;
    }

    /**
     * Get the standard callback implementation.
     */
    public static final AskarLibrary.AskarCallback CALLBACK = new AskarLibrary.AskarCallback() {
        @Override
        public void callback(long callbackId, int errorCode, Pointer result) {
            CompletableFuture<Pointer> future = pendingCallbacks.remove(callbackId);
            if (future != null) {
                if (errorCode == 0) {
                    future.complete(result);
                } else {
                    try {
                        checkError(errorCode);
                        future.complete(result);
                    } catch (AskarException e) {
                        future.completeExceptionally(e);
                    }
                }
            }
        }
    };

    /**
     * Convert a string to InputBuffer for sending to native library.
     * @param str The string to convert
     * @return InputBuffer representation
     */
    public static AskarLibrary.InputBuffer stringToInputBuffer(String str) {
        return new AskarLibrary.InputBuffer(str);
    }

    /**
     * Convert bytes to InputBuffer for sending to native library.
     * @param bytes The bytes to convert
     * @return InputBuffer representation
     */
    public static AskarLibrary.InputBuffer bytesToInputBuffer(byte[] bytes) {
        return new AskarLibrary.InputBuffer(bytes);
    }

    /**
     * Convert a string to RawBuffer (legacy - use stringToInputBuffer for new code).
     * @param str The string to convert
     * @return RawBuffer representation
     * @deprecated Use stringToInputBuffer instead
     */
    @Deprecated
    public static AskarLibrary.RawBuffer stringToRawBuffer(String str) {
        return new AskarLibrary.RawBuffer(str);
    }

    /**
     * Convert bytes to RawBuffer (legacy - use bytesToInputBuffer for new code).
     * @param bytes The bytes to convert
     * @return RawBuffer representation
     * @deprecated Use bytesToInputBuffer instead
     */
    @Deprecated
    public static AskarLibrary.RawBuffer bytesToRawBuffer(byte[] bytes) {
        return new AskarLibrary.RawBuffer(bytes);
    }

    /**
     * Free an OutputBuffer that was allocated by Rust.
     * ONLY use this for buffers returned from native library functions.
     * @param buffer The output buffer to free
     */
    public static void freeOutputBuffer(AskarLibrary.OutputBuffer buffer) {
        if (buffer != null && buffer.isValid()) {
            // Cast to RawBuffer for FFI compatibility
            AskarLibrary.RawBuffer rawBuffer = new AskarLibrary.RawBuffer();
            rawBuffer.len = buffer.len;
            rawBuffer.data = buffer.data;
            AskarLibrary.INSTANCE.askar_buffer_free(rawBuffer);
        }
    }

    /**
     * Free an InputBuffer's Java-allocated memory.
     * @param buffer The input buffer to clean up
     */
    public static void freeInputBuffer(AskarLibrary.InputBuffer buffer) {
        if (buffer != null) {
            buffer.freeJavaMemory();
        }
    }

    /**
     * Free a RawBuffer (legacy method - DANGEROUS).
     * @param buffer The buffer to free
     * @deprecated DO NOT USE - this method is unsafe and can cause crashes
     */
    @Deprecated
    public static void freeBuffer(AskarLibrary.RawBuffer buffer) {
        // Do nothing - this method was causing crashes by freeing Java memory with Rust deallocator
        logger.warn("freeBuffer() called - this method is deprecated and unsafe. Use freeInputBuffer() or freeOutputBuffer() instead.");
    }

    /**
     * Free a string pointer.
     * @param ptr The string pointer to free
     */
    public static void freeString(Pointer ptr) {
        if (ptr != null && ptr != Pointer.NULL) {
            AskarLibrary.INSTANCE.askar_string_free(ptr);
        }
    }

    /**
     * Get next callback ID for async operations.
     * @return The next callback ID
     */
    public static long getNextCallbackId() {
        return callbackCounter.getAndIncrement();
    }

    /**
     * Register a callback for an async operation.
     * @param callbackId The callback ID
     * @param future The future to complete when the callback is called
     */
    public static void registerCallback(long callbackId, CompletableFuture<Pointer> future) {
        pendingCallbacks.put(callbackId, future);
    }

    /**
     * Set the maximum log level for the library.
     * @param level The log level (1=ERROR, 2=WARN, 3=INFO, 4=DEBUG)
     */
    public static void setMaxLogLevel(int level) {
        ensureInitialized();
        try {
            checkError(AskarLibrary.INSTANCE.askar_set_max_log_level(level));
        } catch (AskarException e) {
            logger.warn("Failed to set log level", e);
        }
    }

    /**
     * Shutdown the library and cleanup resources.
     */
    public static void shutdown() {
        if (initialized) {
            AskarLibrary.INSTANCE.askar_terminate();
            pendingCallbacks.clear();
            initialized = false;
        }
    }

    static {
        // Add shutdown hook to cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(LibraryLoader::shutdown));
    }
}