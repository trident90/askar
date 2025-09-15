# Aries-Askar Java Wrapper

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/openwallet-foundation/askar/blob/master/LICENSE-APACHE) [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://github.com/openwallet-foundation/askar/blob/master/LICENSE-MIT)

A Java wrapper around the `aries-askar` Rust library, providing secure storage support for Hyperledger Aries agents.

## Requirements

- Java 8 or higher
- Native library (libaries_askar.so on Linux, libaries_askar.dylib on macOS, aries_askar.dll on Windows)

## Installation

### Maven

```xml
<dependency>
    <groupId>org.hyperledger.aries</groupId>
    <artifactId>aries-askar</artifactId>
    <version>0.4.5</version>
</dependency>
```

### Gradle

```gradle
implementation 'org.hyperledger.aries:aries-askar:0.4.5'
```

## Quick Start

```java
import org.hyperledger.aries.askar.*;

// Provision a new store
Store store = Store.provision("sqlite:///tmp/test.db", "raw", null, null, false);

// Open a session
try (Session session = store.session().open()) {
    // Insert a record
    session.insert("category", "name", "value".getBytes(), null, null);

    // Fetch a record
    Entry entry = session.fetch("category", "name", false);
    System.out.println(new String(entry.getValue()));
}

// Close store
store.close(false);
```

## Features

- **Store Management**: Create, open, and manage secure storage instances
- **Session Management**: Transaction and non-transaction session support
- **Key Management**: Generate, store, and manage cryptographic keys
- **Encryption/Decryption**: AEAD encryption with various key algorithms
- **Digital Signatures**: Sign and verify messages
- **Key Exchange**: ECDH key exchange operations
- **Scanning**: Efficient scanning through large datasets

## Core Classes

- `Store`: Main store interface for provisioning and opening stores
- `Session`: Session interface for database operations
- `Key`: Cryptographic key management
- `Entry`: Store entry representation
- `AskarException`: Exception handling

## License

Licensed under either of:

- Apache License, Version 2.0 ([LICENSE-APACHE](https://github.com/openwallet-foundation/askar/blob/master/LICENSE-APACHE))
- MIT license ([LICENSE-MIT](https://github.com/openwallet-foundation/askar/blob/master/LICENSE-MIT))

at your option.