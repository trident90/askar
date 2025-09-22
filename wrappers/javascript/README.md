# Aries Askar JavaScript Wrapper

A Node.js FFI wrapper for the [Aries Askar](https://github.com/hyperledger/aries-askar) secure storage library using [Koffi](https://github.com/Koromix/koffi).

## ✅ Node.js v20+ Compatible

This wrapper uses **Koffi v2.14.1** for FFI bindings, which provides:
- ✅ Full Node.js v20+ compatibility
- ✅ Better performance than ffi-napi
- ✅ Modern TypeScript support
- ✅ No native compilation required
- ✅ Cross-platform support

## Features

- **Secure Storage**: SQLite-based encrypted storage with profile support
- **Key Management**: Generate, store, and manage cryptographic keys
- **Multiple Algorithms**: Support for Ed25519, X25519, P-256, P-384, P-521, BLS12-381, and more
- **AEAD Encryption**: Authenticated encryption with additional data
- **Digital Signatures**: Sign and verify messages
- **Key Exchange**: ECDH-ES and ECDH-1PU key derivation
- **Crypto Box**: NaCl-style encryption/decryption
- **Transactions**: Atomic operations with rollback support
- **Async/Promise-based API**: Modern JavaScript async/await support

## Prerequisites

- Node.js 16+ (tested with Node.js 16, 18, 20+)
- The Aries Askar native library must be built and available

## Quick Setup

Use the setup script to build everything automatically:

```bash
# Run the setup script
./scripts/setup.sh
```

Or follow manual steps below.

## Manual Setup

### 1. Build the Native Library

Before using this wrapper, build the Askar library with SQLite support:

```bash
# From the root askar directory
cd ../..
cargo build --release --features sqlite
```

**Important**: The `sqlite` feature is required for store operations. This creates `libaries_askar.so` (Linux), `libaries_askar.dylib` (macOS), or `aries_askar.dll` (Windows) in `target/release/`.

### 2. Install and Build JavaScript Wrapper

```bash
npm install
npm run build
```

### 3. Alternative: Use npm scripts

```bash
# Builds native library with SQLite + TypeScript
npm run pretest

# Or build components separately
npm run build-native  # Builds Rust library with SQLite
npm run build         # Builds TypeScript
```

## Quick Start

```javascript
const { Store, LocalKey, KeyAlg, Askar } = require('./lib');

async function example() {
  // Set up logging
  Askar.setDefaultLogger();

  // Create or open a store
  const store = await Store.provision('sqlite://test.db', 'raw', 'test-key');

  // Start a session
  const session = await store.startSession();

  // Store some data
  await session.insert('category', 'name', Buffer.from('data'), {
    tag1: 'value1'
  });

  // Retrieve data
  const entry = await session.fetch('category', 'name');
  console.log('Value:', entry.value.toString());

  // Generate a key
  const key = LocalKey.generate(KeyAlg.Ed25519);

  // Store the key
  await session.insertKey(key, 'my-key', 'metadata', { purpose: 'signing' });

  // Sign a message
  const signature = key.signMessage('Hello, World!');
  const isValid = key.verifySignature('Hello, World!', signature);
  console.log('Signature valid:', isValid);

  // Clean up
  key.free();
  await session.close();
  await store.close();
}

example().catch(console.error);
```

## Core Classes

### Store

The main entry point for working with Askar storage.

```javascript
// Create a new store
const store = await Store.provision(uri, keyMethod, passKey, profile, recreate);

// Open existing store
const store = await Store.open(uri, keyMethod, passKey, profile);

// Store operations
await store.rekey(newKeyMethod, newPassKey);
const backupStore = await store.copy(targetUri, keyMethod, passKey);
await Store.remove(uri);
```

### Session

Provides read/write access to store data.

```javascript
const session = await store.startSession(profile, asTransaction);

// Data operations
await session.insert(category, name, value, tags, expiryMs);
await session.replace(category, name, value, tags, expiryMs);
const entry = await session.fetch(category, name, forUpdate);
const entries = await session.fetchAll(category, tagFilter, limit, orderBy);
await session.remove(category, name);

// Key operations
await session.insertKey(key, name, metadata, tags, expiryMs);
const keyEntry = await session.fetchKey(name, forUpdate);
const keyEntries = await session.fetchAllKeys(algorithm, thumbprint, tagFilter);
await session.updateKey(name, metadata, tags, expiryMs);
await session.removeKey(name);

await session.close(commit);
```

### LocalKey

Cryptographic key management and operations.

```javascript
// Generate keys
const key = LocalKey.generate(KeyAlg.Ed25519, KeyBackend.Software, ephemeral);
const keyFromSeed = LocalKey.fromSeed(algorithm, seed, method);
const keyFromJwk = LocalKey.fromJwk(jwkObject);

// Key properties
const algorithm = key.getAlgorithm();
const isEphemeral = key.isEphemeral();
const jwkPublic = key.getJwkPublic();
const jwkSecret = key.getJwkSecret();
const thumbprint = key.getJwkThumbprint();

// Cryptographic operations
const signature = key.signMessage(message, sigType);
const isValid = key.verifySignature(message, signature, sigType);

const encrypted = key.aeadEncrypt(message, nonce, aad);
const decrypted = key.aeadDecrypt(ciphertext, nonce, tag, aad);

const wrapped = key.wrapKey(otherKey, nonce);
const unwrapped = key.unwrapKey(algorithm, ciphertext, nonce, tag);

// Always free keys when done
key.free();
```

### Scan

Efficient iteration over large datasets.

```javascript
const scan = await store.startScan(profile, category, tagFilter, offset, limit);

let batch;
while ((batch = await scan.next()).length > 0) {
  batch.forEach(entry => {
    console.log(entry.name, entry.value);
  });
}

scan.free();
```

## Supported Algorithms

### Signing Algorithms
- `Ed25519`: EdDSA signatures using Curve25519
- `Secp256k1`: ECDSA using secp256k1 curve
- `P256`, `P384`, `P521`: ECDSA using NIST P curves
- `Bls12381G1`, `Bls12381G2`: BLS signatures

### Key Agreement Algorithms
- `X25519`: ECDH using Curve25519
- `P256`, `P384`, `P521`: ECDH using NIST P curves

### Symmetric Encryption
- `A128Gcm`, `A256Gcm`: AES-GCM
- `A128CbcHs256`, `A256CbcHs512`: AES-CBC with HMAC
- `C20P`, `XC20P`: ChaCha20-Poly1305
- `A128Kw`, `A192Kw`, `A256Kw`: AES Key Wrap

## Error Handling

The wrapper throws `AskarError` instances with detailed error information:

```javascript
try {
  await session.fetch('nonexistent', 'key');
} catch (error) {
  if (error instanceof AskarError) {
    console.log('Error code:', error.code);
    console.log('Error message:', error.message);
  }
}
```

## Examples

See the `examples/` directory for comprehensive usage examples:

- `basic.js`: Simple store operations and key management
- `advanced.js`: Profiles, transactions, key exchange, and scanning

Run examples:

```bash
npm run build
node examples/basic.js
```

## Library Path Configuration

The wrapper automatically detects the Askar library location. You can override this by setting the library path before importing:

```javascript
process.env.ASKAR_LIB_PATH = '/path/to/libaries_askar.so';
const askar = require('aries-askar');
```

## Memory Management

Important: Always free resources when done to prevent memory leaks:

```javascript
try {
  const key = LocalKey.generate(KeyAlg.Ed25519);
  const store = await Store.open('sqlite://test.db', 'raw', 'key');
  const session = await store.startSession();

  // ... use resources ...

} finally {
  // Clean up in reverse order
  if (key) key.free();
  if (session) await session.close();
  if (store) await store.close();
}
```

## Building

```bash
# Install dependencies
npm install

# Build TypeScript
npm run build

# Run tests (when available)
npm test
```

## Troubleshooting

See [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) for common issues and solutions.

### Common Issues

1. **Library not found**: Set `ASKAR_LIB_PATH` environment variable
2. **Memory leaks**: Always call `.free()` on keys and close sessions/stores
3. **Async errors**: Use proper error handling with try/catch blocks

## Contributing

Contributions are welcome! Please ensure that:

1. Code follows TypeScript best practices
2. All tests pass
3. Documentation is updated for new features
4. Examples demonstrate new functionality

## Why Koffi?

We migrated from `ffi-napi` to Koffi because:

- **Node.js v20+ Support**: Koffi works with all modern Node.js versions
- **Better Performance**: Faster function calls and lower overhead
- **No Compilation**: No need for node-gyp or native compilation
- **TypeScript Native**: Built with TypeScript support from the ground up
- **Active Development**: Regularly updated with new features

## License

Apache License 2.0 - See LICENSE file for details.

## Related Projects

- [Aries Askar](https://github.com/hyperledger/aries-askar) - The main library
- [Python Wrapper](../python/) - Fully functional Python bindings
- [Java Wrapper](../java/) - Java JNI bindings
- [Koffi](https://github.com/Koromix/koffi) - Fast and simple FFI for Node.js