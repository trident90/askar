package org.hyperledger.aries.askar;

/**
 * JNI interface to the native Askar library.
 * This provides direct access to Rust FFI functions through JNI.
 */
public class AskarNative {
    
    static {
        try {
            System.loadLibrary("askar_minimal_test");
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("Failed to load askar_minimal_test native library", e);
        }
    }
    
    // Library management
    public static native String getVersion();
    public static native void freeString(long stringPtr);
    public static native int setMaxLogLevel(int level);
    public static native void terminate();
    
    // Store operations
    public static native long storeProvision(
        String uri, 
        String keyMethod, 
        String passKey, 
        String profile, 
        boolean recreate
    );
    
    public static native long storeOpen(
        String uri,
        String keyMethod, 
        String passKey,
        String profile
    );
    
    public static native void storeClose(long storeHandle);
    
    // Session operations  
    public static native long sessionStart(long storeHandle, String profile, boolean asTransaction);
    public static native int sessionCount(long sessionHandle, String category, String tagFilter);
    public static native long sessionFetch(long sessionHandle, String category, String name, boolean forUpdate);
    public static native long sessionFetchAll(
        long sessionHandle,
        String category, 
        String tagFilter,
        int limit,
        String orderBy,
        boolean descending,
        boolean forUpdate
    );
    
    public static native void sessionUpdate(
        long sessionHandle,
        byte operation, 
        String category,
        String name,
        byte[] value,
        String tags,
        long expiryMs
    );
    
    public static native void sessionClose(long sessionHandle, boolean commit);
    
    // Entry list operations
    public static native int entryListCount(long entryListHandle);
    public static native String entryListGetCategory(long entryListHandle, int index);
    public static native String entryListGetName(long entryListHandle, int index);
    public static native byte[] entryListGetValue(long entryListHandle, int index);
    public static native String entryListGetTags(long entryListHandle, int index);
    public static native void entryListFree(long entryListHandle);
    
    // Key operations
    public static native long keyGenerate(String algorithm, String backend, boolean ephemeral);
    public static native long keyFromSeed(String algorithm, byte[] seed, String method);
    public static native void keyFree(long keyHandle);
    
    // Advanced key operations
    public static native String keyGetAlgorithm(long keyHandle);
    public static native byte[] keyGetPublicBytes(long keyHandle);
    public static native byte[] keyGetSecretBytes(long keyHandle);
    public static native String keyGetJwkPublic(long keyHandle, String algorithm);
    public static native byte[] keyGetJwkSecret(long keyHandle);
    public static native long keyFromJwk(byte[] jwkData);
    public static native long keyFromPublicBytes(String algorithm, byte[] publicBytes);
    public static native long keyFromSecretBytes(String algorithm, byte[] secretBytes);
    
    // Cryptographic operations
    public static native byte[] keySignMessage(long keyHandle, byte[] message, String signatureType);
    public static native boolean keyVerifySignature(long keyHandle, byte[] message, byte[] signature, String signatureType);
    public static native byte[] keyAeadEncrypt(long keyHandle, byte[] message, byte[] nonce, byte[] aad);
    public static native byte[] keyAeadDecrypt(long keyHandle, byte[] ciphertext, byte[] nonce, byte[] tag, byte[] aad);
    public static native byte[] keyCryptoBox(long recipientKey, long senderKey, byte[] message, byte[] nonce);
    public static native byte[] keyCryptoBoxOpen(long recipientKey, long senderKey, byte[] ciphertext, byte[] nonce);
    public static native byte[] keyWrapKey(long wrapperKey, long keyToWrap, byte[] nonce);
    public static native long keyUnwrapKey(long wrapperKey, String algorithm, byte[] ciphertext, byte[] nonce, byte[] tag);
    
    // Key derivation operations
    public static native long keyDeriveEcdhEs(String algorithm, long ephemeralKey, long recipientKey, 
                                              byte[] algorithmId, byte[] apu, byte[] apv, boolean receive);
    public static native long keyDeriveEcdh1Pu(String algorithm, long ephemeralKey, long senderKey, long recipientKey,
                                               byte[] algorithmId, byte[] apu, byte[] apv, byte[] ccTag, boolean receive);
    public static native long keyFromKeyExchange(String algorithm, long secretKey, long publicKey);
    public static native long keyConvert(long keyHandle, String algorithm);
    public static native byte[] keyAeadRandomNonce(long keyHandle);
    public static native byte[] keyCryptoBoxRandomNonce();
    
    // Utility functions
    public static native String getLastError();
}