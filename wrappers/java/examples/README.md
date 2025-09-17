# Askar Java Examples

This directory contains examples demonstrating the usage of the Askar Java wrapper.

## Prerequisites

1. **Java 8 or higher**
2. **Maven** (recommended)
3. **SQLite 3** (for store operations)
4. **Askar native library** (libaskar_jni_wrapper.so)

## Quick Start

### 1. Build Everything

```bash
# From parent directory
cd ..
mvn clean compile package -DskipTests
cd examples
mvn clean compile
```

### 2. Setup Environment for Store Operations

```bash
# Source the environment setup script (important: use 'source' or '.')
source setup-store-env.sh

# This script will check and setup:
# ✅ SQLite installation
# ✅ Directory write permissions
# ✅ Database creation capability
# ✅ Java environment (JAVA_HOME)
# ✅ Native library linking
# ✅ Environment variables
```

### 3. Run Examples

#### Key Operations (Always Work)
```bash
# High-level wrapper demo
java -cp "target/classes:../target/aries-askar-0.4.5.jar" \
     -Djava.library.path=../src/main/native \
     org.hyperledger.aries.askar.examples.SimpleHighLevelTest

# Basic API test
java -cp "target/classes:../target/aries-askar-0.4.5.jar" \
     -Djava.library.path=../src/main/native \
     org.hyperledger.aries.askar.examples.SimpleTest

# Comprehensive cryptography
java -cp "target/classes:../target/aries-askar-0.4.5.jar" \
     -Djava.library.path=../src/main/native \
     org.hyperledger.aries.askar.examples.CryptographyExample
```

#### Store Operations (Requires Environment Setup)
```bash
# Make sure you ran: source setup-store-env.sh first!

# Basic store operations
java -cp "target/classes:../target/aries-askar-0.4.5.jar" \
     -Djava.library.path=../src/main/native \
     org.hyperledger.aries.askar.examples.BasicStoreExample

# Complete JNI demonstration
java -cp "target/classes:../target/aries-askar-0.4.5.jar" \
     -Djava.library.path=../src/main/native \
     org.hyperledger.aries.askar.examples.CompleteJNITest
```

## Available Examples

### 1. SimpleHighLevelTest ⭐⭐⭐⭐⭐
**Recommended starting point**
- High-level wrapper demonstration
- Key generation with automatic cleanup
- Digital signatures and verification
- Deterministic keys from seed
- Try-with-resources pattern

**Status**: ✅ Fully working

### 2. SimpleTest ⭐⭐⭐⭐⭐
- Basic API verification
- Key operations testing
- Store operation attempt
- Version information

**Status**: ✅ Key operations work, Store may need setup

### 3. CryptographyExample ⭐⭐⭐⭐⭐
- Comprehensive cryptographic operations
- Multiple key algorithms
- Digital signatures with tamper detection
- Deterministic key generation
- Key format conversions

**Status**: ✅ Fully working

### 4. BasicStoreExample ⭐⭐⭐
- Store provisioning and management
- Data insertion, retrieval, updates
- Tag-based querying
- Profile operations
- Session management

**Status**: ⚠️ Requires environment setup

### 5. CompleteJNITest ⭐⭐⭐
- Complete JNI API demonstration
- All major operations showcase
- Error handling examples
- Performance testing

**Status**: ⚠️ Requires environment setup

## Environment Setup Details

The `setup-store-env.sh` script performs these checks:

### 1. SQLite Installation Check
```bash
sqlite3 --version
```

### 2. Directory Permissions Test
```bash
touch test_write.tmp && rm test_write.tmp
```

### 3. Database Creation Test
```bash
sqlite3 test.db "CREATE TABLE test(id INTEGER); SELECT 1;" && rm test.db
```

### 4. Native Library Verification
```bash
ls -la ../src/main/native/libaskar_jni_wrapper.so
ldd ../src/main/native/libaskar_jni_wrapper.so | grep libaries_askar
```

### 5. Environment Variables
- `JAVA_HOME`: Java installation path
- `LD_LIBRARY_PATH`: Native library search path
- `RUST_LOG`: Logging level (warn/debug)

## Troubleshooting

### 1. "UnsatisfiedLinkError: no askar_jni_wrapper"
```
Solution: Ensure native library is built and library path is correct
cd ../src/main/native && ./build.sh
```

### 2. "symbol lookup error: undefined symbol: askar_*"
```
Solution: Native library found but Askar core not linked
cd ../src/main/native && export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 && ./build.sh
```

### 3. Store provisioning fails
```
Solution: Check environment setup
source setup-store-env.sh
```

### 4. SQLite errors
```
Solution: Install SQLite development libraries
# Ubuntu/Debian
sudo apt install sqlite3 libsqlite3-dev

# CentOS/RHEL
sudo yum install sqlite sqlite-devel
```

### 5. Permission denied errors
```
Solution: Check directory permissions
chmod 755 .
mkdir -p test_data && chmod 755 test_data
```

## Manual Environment Setup

If you prefer to set up manually instead of using the script:

```bash
# 1. Install SQLite
sudo apt install sqlite3 libsqlite3-dev  # Ubuntu/Debian

# 2. Set Java environment
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

# 3. Build native library
cd ../src/main/native && ./build.sh && cd -

# 4. Set library path
export LD_LIBRARY_PATH="../src/main/native:/home/ubuntu/Projects/DID/askar/target/release:$LD_LIBRARY_PATH"

# 5. Set logging (optional)
export RUST_LOG=warn  # or 'debug' for verbose output

# 6. Create test directory
mkdir -p test_data
```

## Running with Debug Output

For troubleshooting store operations:

```bash
export RUST_LOG=debug
java -cp "target/classes:../target/aries-askar-0.4.5.jar" \
     -Djava.library.path=../src/main/native \
     org.hyperledger.aries.askar.examples.BasicStoreExample
```

## Expected Output

### ✅ Key Operations (Always Work)
```
=== Simple High-Level Wrapper Test ===
✅ Generated Ed25519 key: ed25519
✅ Message signed, signature length: 64 bytes  
✅ Signature verification: VALID
🎉 High-level wrapper test completed successfully!
```

### ⚠️ Store Operations (Additional Setup Required)
```
=== Memory Store Test ===
✅ Askar version: 0.4.5
⚠️  Store provisioning: Requires additional Askar core setup
   URI: sqlite://:memory:
   Status: All store operations currently return handle 0

Note: Key operations work perfectly, store operations need investigation
```

## Additional Resources

- [Developer Manual](../DEVELOPER_MANUAL.md) - Complete API documentation
- [Native Library Build Guide](../src/main/native/README.md)
- [Troubleshooting Guide](../DEVELOPER_MANUAL.md#troubleshooting)

## Files in This Directory

- `setup-store-env.sh` - Environment setup script for store operations
- `src/main/java/.../*.java` - Example source files
- `pom.xml` - Maven build configuration
- `target/` - Compiled classes (after build)
- `test_data/` - Test database files (created by examples)