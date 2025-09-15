# Changelog

All notable changes to the Aries Askar Java wrapper will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.4.5] - 2024-01-XX

### Added
- Initial release of Java wrapper for Aries Askar
- Complete Store API implementation with async operations
- Session management for database operations
- Key management with cryptographic operations
- Support for multiple key algorithms (Ed25519, secp256k1, X25519, etc.)
- Digital signature creation and verification
- AEAD encryption/decryption operations
- Key exchange capabilities
- JWK format support for key import/export
- Multi-profile store support
- Transaction support with commit/rollback
- Entry and key entry management
- Comprehensive error handling with AskarException
- Memory management with proper resource cleanup
- JNA-based native library integration
- Maven and Gradle build support
- Comprehensive test suite
- Documentation and examples
- Support for SQLite and PostgreSQL backends

### Key Features
- **Store Operations**: Provision, open, close, remove stores
- **Session Management**: Non-transactional and transactional sessions
- **Data Storage**: Insert, fetch, update, remove entries with tags
- **Key Management**: Generate, store, retrieve cryptographic keys
- **Cryptography**: Sign/verify messages, encrypt/decrypt data
- **Profiles**: Multi-tenant support with isolated data spaces
- **Scanning**: Efficient iteration through large datasets
- **Raw Key Generation**: Deterministic key derivation from seeds

### Supported Algorithms
- **Signing**: Ed25519, Ed448, ES256, ES256K, ES384, ES512
- **Key Exchange**: X25519, X448
- **Encryption**: ChaCha20-Poly1305, AES-128-GCM, AES-256-GCM
- **Key Wrapping**: AES-128-KW, AES-256-KW
- **Advanced**: BLS12-381 signatures, RSA

### Dependencies
- JNA 5.14.0 for native library access
- Jackson 2.16.1 for JSON processing
- SLF4J 2.0.9 for logging
- JUnit 5.10.1 for testing

### Platform Support
- Linux (x86_64, ARM64)
- macOS (Intel, Apple Silicon)
- Windows (x86_64)

### API Highlights

#### Basic Store Usage
```java
Store store = Store.provision("sqlite:///tmp/test.db", "raw", null, null, false);
try (Session session = store.session().open()) {
    session.insert("category", "name", "value".getBytes(), null, null);
    Entry entry = session.fetch("category", "name", false);
}
store.close();
```

#### Key Management
```java
Key key = Key.generate(KeyAlgorithm.ED25519, false);
byte[] signature = key.signMessage("message".getBytes(), null);
boolean valid = key.verifySignature("message".getBytes(), signature, null);
```

#### Transactions
```java
try (Session txn = store.transaction().open()) {
    txn.insert("category", "name", "value".getBytes(), null, null);
    txn.commit(); // or txn.rollback()
}
```

### Notes
- Requires native Askar library (libaries_askar.so/.dylib/.dll)
- Java 8+ compatibility
- Thread-safe operations through native library
- Automatic resource management with try-with-resources
- Comprehensive error handling and reporting

### Breaking Changes
N/A - Initial release

### Migration Guide
N/A - Initial release

### Known Issues
- None at time of release

### Acknowledgments
- Based on the Python wrapper implementation
- Uses the Aries Askar Rust library for all cryptographic operations
- Follows Java conventions and best practices for wrapper libraries