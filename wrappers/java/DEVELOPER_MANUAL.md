# Askar Java Wrapper Developer Manual

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Setup and Build](#setup-and-build)
4. [API Reference](#api-reference)
5. [Usage Examples](#usage-examples)
6. [Development Guidelines](#development-guidelines)
7. [Troubleshooting](#troubleshooting)
8. [Contributing](#contributing)

## Overview

The Askar Java Wrapper provides JNI (Java Native Interface) bindings for the Aries Askar cryptographic storage library. This wrapper enables Java applications to use Askar's secure key management, storage operations, and cryptographic functions.

### Features

- **Store Management**: Create, provision, and manage encrypted storage
- **Session Operations**: Handle database transactions and queries
- **Key Management**: Generate, store, and manage cryptographic keys
- **Advanced Cryptography**: Sign, verify, encrypt, and decrypt operations
- **Key Derivation**: ECDH-ES and ECDH-1PU key derivation
- **Scan Operations**: Iterate through stored entries

## Architecture

### Components

```
┌─────────────────┐
│  Java Layer     │  AskarNative.java (JNI declarations)
├─────────────────┤
│  JNI Layer      │  askar_minimal_test.c (C implementation)
├─────────────────┤
│  Rust FFI       │  Askar Core Library
└─────────────────┘
```

### Key Classes

- **`AskarNative`**: Main JNI interface with native method declarations
- **`Store`**: High-level store management wrapper
- **`Session`**: Transaction and query management
- **`Key`**: Cryptographic key operations

## Setup and Build

### Prerequisites

- Java 11 or higher
- GCC compiler
- Rust toolchain
- CMake (optional)

### Building the Native Library

1. **Build Askar Core Library**:
   ```bash
   cd /path/to/askar
   cargo build --release
   ```

2. **Compile JNI Wrapper**:
   ```bash
   cd wrappers/java/src/main/native
   gcc -shared -fPIC -o libaskar_minimal_test.so askar_minimal_test.c \
       -I/usr/lib/jvm/java-11-openjdk-amd64/include \
       -I/usr/lib/jvm/java-11-openjdk-amd64/include/linux \
       -L/path/to/askar/target/release -laries_askar
   ```

3. **Copy Library to Resources**:
   ```bash
   cp libaskar_minimal_test.so ../resources/
   ```

### Java Compilation

```bash
javac -d target/classes src/main/java/org/hyperledger/aries/askar/*.java
```

## API Reference

### Store Operations

#### Store Provisioning
```java
public static native long storeProvision(
    String uri,           // Database URI (e.g., "sqlite://database.db")
    String keyMethod,     // Key derivation method ("raw", "kdf:argon2i:mod")
    String passKey,       // Password or raw key
    String profile,       // Profile name (usually "default")
    boolean recreate      // Whether to recreate if exists
);
```

#### Store Management
```java
public static native long storeOpen(String uri, String keyMethod, String passKey, String profile);
public static native void storeClose(long storeHandle);
public static native void storeRekey(long storeHandle, String keyMethod, String passKey);
public static native boolean storeRemove(String uri);
public static native long storeCopyTo(long storeHandle, String targetUri, String keyMethod, String passKey, boolean recreate);
```

#### Profile Management
```java
public static native String storeCreateProfile(long storeHandle, String profile);
public static native boolean storeRemoveProfile(long storeHandle, String profile);
public static native String storeGetProfileName(long storeHandle);
public static native String storeGetDefaultProfile(long storeHandle);
public static native void storeSetDefaultProfile(long storeHandle, String profile);
public static native String[] storeListProfiles(long storeHandle);
```

### Session Operations

#### Session Management
```java
public static native long sessionStart(long storeHandle, String profile, boolean asTransaction);
public static native void sessionClose(long sessionHandle, boolean commit);
```

#### Data Operations
```java
public static native void sessionUpdate(
    long sessionHandle,
    byte operation,       // 1=Insert, 2=Replace, 3=Remove
    String category,
    String name,
    byte[] value,
    String tags,         // JSON format: {"tag1": "value1", "tag2": "value2"}
    long expiryMs        // Expiry timestamp in milliseconds, 0 for no expiry
);

public static native long sessionFetch(long sessionHandle, String category, String name, boolean forUpdate);
public static native long sessionFetchAll(long sessionHandle, String category, String tagFilter, int limit, String orderBy, boolean descending, boolean forUpdate);
public static native int sessionCount(long sessionHandle, String category, String tagFilter);
```

#### Advanced Session Operations
```java
public static native void sessionInsertKey(long sessionHandle, long keyHandle, String name, String metadata, String tags, long expiryMs);
public static native long sessionFetchKey(long sessionHandle, String name, boolean forUpdate);
public static native void sessionUpdateKey(long sessionHandle, String name, String metadata, String tags, long expiryMs);
public static native void sessionRemoveKey(long sessionHandle, String name);
public static native long sessionFetchAllKeys(long sessionHandle, String tagFilter, int limit, boolean forUpdate);
public static native long sessionRemoveAll(long sessionHandle, String category, String tagFilter);
```

### Key Operations

#### Key Generation
```java
public static native long keyGenerate(String algorithm, String backend, boolean ephemeral);
public static native long keyFromSeed(String algorithm, byte[] seed, String method);
public static native long keyFromPublicBytes(String algorithm, byte[] publicBytes);
public static native long keyFromSecretBytes(String algorithm, byte[] secretBytes);
public static native long keyFromJwk(byte[] jwkData);
```

#### Key Information
```java
public static native String keyGetAlgorithm(long keyHandle);
public static native byte[] keyGetPublicBytes(long keyHandle);
public static native byte[] keyGetSecretBytes(long keyHandle);
public static native String keyGetJwkPublic(long keyHandle, String algorithm);
public static native byte[] keyGetJwkSecret(long keyHandle);
```

#### Cryptographic Operations
```java
public static native byte[] keySignMessage(long keyHandle, byte[] message, String signatureType);
public static native boolean keyVerifySignature(long keyHandle, byte[] message, byte[] signature, String signatureType);
public static native byte[] keyAeadEncrypt(long keyHandle, byte[] message, byte[] nonce, byte[] aad);
public static native byte[] keyAeadDecrypt(long keyHandle, byte[] ciphertext, byte[] nonce, byte[] tag, byte[] aad);
```

#### Key Derivation
```java
public static native long keyDeriveEcdhEs(String algorithm, long ephemeralKey, long recipientKey, byte[] algorithmId, byte[] apu, byte[] apv, boolean receive);
public static native long keyDeriveEcdh1Pu(String algorithm, long ephemeralKey, long senderKey, long recipientKey, byte[] algorithmId, byte[] apu, byte[] apv, byte[] ccTag, boolean receive);
```

### Entry List Operations

#### Working with Entry Lists
```java
public static native int entryListCount(long entryListHandle);
public static native String entryListGetCategory(long entryListHandle, int index);
public static native String entryListGetName(long entryListHandle, int index);
public static native byte[] entryListGetValue(long entryListHandle, int index);
public static native String entryListGetTags(long entryListHandle, int index);
public static native void entryListFree(long entryListHandle);
```

#### Key Entry List Operations
```java
public static native int keyEntryListCount(long keyEntryListHandle);
public static native String keyEntryListGetAlgorithm(long keyEntryListHandle, int index);
public static native String keyEntryListGetName(long keyEntryListHandle, int index);
public static native String keyEntryListGetMetadata(long keyEntryListHandle, int index);
public static native String keyEntryListGetTags(long keyEntryListHandle, int index);
public static native long keyEntryListLoadKey(long keyEntryListHandle, int index);
public static native void keyEntryListFree(long keyEntryListHandle);
```

### Scan Operations

```java
public static native long scanStart(long storeHandle, String profile, String category, String tagFilter, long offset, long limit, String orderBy, boolean descending);
public static native long scanNext(long scanHandle);
public static native void scanFree(long scanHandle);
```

## Usage Examples

### Basic Store Operations

```java
// Provision a new store
long storeHandle = AskarNative.storeProvision(
    "sqlite://mystore.db", 
    "raw", 
    "my_secret_key", 
    "default", 
    true
);

// Start a session
long sessionHandle = AskarNative.sessionStart(storeHandle, "default", false);

// Store some data
AskarNative.sessionUpdate(
    sessionHandle,
    (byte)1,                    // Insert operation
    "credentials",              // Category
    "user_credential_1",        // Name
    "credential_data".getBytes(), // Value
    "{\"type\": \"credential\", \"issuer\": \"example_org\"}", // Tags
    0                           // No expiry
);

// Fetch the data
long entryList = AskarNative.sessionFetch(sessionHandle, "credentials", "user_credential_1", false);
if (entryList != 0) {
    byte[] value = AskarNative.entryListGetValue(entryList, 0);
    String tags = AskarNative.entryListGetTags(entryList, 0);
    AskarNative.entryListFree(entryList);
}

// Close session and store
AskarNative.sessionClose(sessionHandle, true);
AskarNative.storeClose(storeHandle);
```

### Key Management

```java
// Generate a signing key
long signingKey = AskarNative.keyGenerate("ed25519", null, false);

// Get key information
String algorithm = AskarNative.keyGetAlgorithm(signingKey);
byte[] publicBytes = AskarNative.keyGetPublicBytes(signingKey);

// Store the key
AskarNative.sessionInsertKey(
    sessionHandle,
    signingKey,
    "my_signing_key",
    "{\"purpose\": \"document_signing\"}",
    "{\"type\": \"signing\", \"curve\": \"ed25519\"}",
    0
);

// Sign a message
byte[] message = "Hello, World!".getBytes();
byte[] signature = AskarNative.keySignMessage(signingKey, message, null);

// Verify signature
boolean isValid = AskarNative.keyVerifySignature(signingKey, message, signature, null);

// Clean up
AskarNative.keyFree(signingKey);
```

### Working with Key Entry Lists

```java
// Fetch all keys
long keyEntryList = AskarNative.sessionFetchAllKeys(sessionHandle, null, 10, false);

int keyCount = AskarNative.keyEntryListCount(keyEntryList);
for (int i = 0; i < keyCount; i++) {
    String name = AskarNative.keyEntryListGetName(keyEntryList, i);
    String algorithm = AskarNative.keyEntryListGetAlgorithm(keyEntryList, i);
    String metadata = AskarNative.keyEntryListGetMetadata(keyEntryList, i);
    
    // Load the actual key
    long keyHandle = AskarNative.keyEntryListLoadKey(keyEntryList, i);
    // Use the key...
    AskarNative.keyFree(keyHandle);
}

AskarNative.keyEntryListFree(keyEntryList);
```

### Query with Tags

```java
// Store data with tags
AskarNative.sessionUpdate(
    sessionHandle,
    (byte)1,
    "documents",
    "contract_1",
    contractData,
    "{\"type\": \"contract\", \"status\": \"active\", \"year\": \"2024\"}",
    0
);

// Query by tags
long results = AskarNative.sessionFetchAll(
    sessionHandle,
    "documents",
    "{\"type\": \"contract\", \"status\": \"active\"}", // Tag filter
    100,        // Limit
    null,       // Order by
    false,      // Descending
    false       // For update
);

int count = AskarNative.entryListCount(results);
for (int i = 0; i < count; i++) {
    String name = AskarNative.entryListGetName(results, i);
    byte[] value = AskarNative.entryListGetValue(results, i);
    // Process results...
}

AskarNative.entryListFree(results);
```

## Development Guidelines

### Error Handling

All native methods can throw exceptions. Always wrap calls in try-catch blocks:

```java
try {
    long storeHandle = AskarNative.storeProvision(uri, keyMethod, passKey, profile, recreate);
} catch (Exception e) {
    // Handle error
    System.err.println("Store provisioning failed: " + e.getMessage());
}
```

### Resource Management

**Critical**: Always free native resources to prevent memory leaks:

```java
// Always free handles when done
AskarNative.keyFree(keyHandle);
AskarNative.entryListFree(entryListHandle);
AskarNative.keyEntryListFree(keyEntryListHandle);
AskarNative.scanFree(scanHandle);
AskarNative.sessionClose(sessionHandle, commit);
AskarNative.storeClose(storeHandle);
```

### Tag Format

Tags must be valid JSON strings:

```java
// Correct
String tags = "{\"type\": \"credential\", \"issuer\": \"example\"}";

// Incorrect
String tags = "type=credential,issuer=example";
```

### Supported Algorithms

- **Signing**: `ed25519`, `secp256k1`, `secp256r1` (p256), `secp384r1` (p384)
- **Encryption**: `x25519`, `p256`, `p384`
- **Symmetric**: `aes128-gcm`, `aes256-gcm`, `chacha20-poly1305`

### Key Derivation Methods

- **raw**: Direct key usage
- **kdf:argon2i:mod**: Argon2i key derivation (moderate)
- **kdf:argon2i:int**: Argon2i key derivation (interactive)

## Troubleshooting

### Common Issues

1. **UnsatisfiedLinkError**: Native library not found
   - Ensure `libaskar_minimal_test.so` is in the library path
   - Set `-Djava.library.path=/path/to/library`

2. **Store provisioning fails**: 
   - Check database URI format
   - Verify write permissions
   - Ensure key method is valid

3. **Key generation fails**:
   - Verify algorithm name is correct
   - Check if backend supports the algorithm

4. **Memory leaks**:
   - Always call free methods for native handles
   - Use try-finally blocks for cleanup

### Debug Mode

Enable debug output by setting the log level:

```java
AskarNative.setMaxLogLevel(4); // Debug level
```

### Performance Tips

1. **Batch operations**: Use transactions for multiple operations
2. **Connection pooling**: Reuse store handles when possible
3. **Tag indexing**: Structure tags for efficient queries
4. **Memory management**: Free resources immediately after use

## Contributing

### Adding New Functions

1. **Declare in Java**: Add native method to `AskarNative.java`
2. **Implement in C**: Add JNI function to `askar_minimal_test.c`
3. **Follow patterns**: Use existing callback and error handling patterns
4. **Test thoroughly**: Create comprehensive tests
5. **Document**: Update this manual

### Code Style

- Use consistent naming: `functionName` in Java, `function_name` in C
- Add debug printf statements for troubleshooting
- Handle all error cases with proper cleanup
- Follow existing memory management patterns

### Testing

Create comprehensive tests that cover:
- Happy path scenarios
- Error conditions
- Resource cleanup
- Memory management
- Edge cases

---

## Version History

- **v1.0.0**: Initial implementation with core store, session, and key operations
- **v1.1.0**: Added advanced session operations and scan functionality
- **v1.2.0**: Added key entry list operations and additional utilities

## License

This project is licensed under the same license as Aries Askar.

For more information, visit the [Aries Askar repository](https://github.com/hyperledger/aries-askar).