# Askar JNI Native Implementation

This directory contains the native C implementation for the Askar Java wrapper.

## Files

### Source Files
- **`askar_jni_wrapper.c`** - Complete JNI implementation (3,096 lines)
  - 67 JNI functions for all Askar operations
  - Multithreaded callback system for async Rust FFI
  - Memory management and error handling
  - Production-ready code (not a test file despite the output library name)

- **`askar_ffi.h`** - Rust FFI header file
  - Definitions for Askar core library functions
  - Data structures and error codes

### Build Tools
- **`build.sh`** - Automated build script (recommended)
  - Automatically detects Java paths
  - Handles compilation and library placement
  - No CMake required

### Generated Files
- **`libaskar_minimal_test.so`** - Compiled JNI library
  - Used by Java code via `System.loadLibrary("askar_minimal_test")`
  - Contains all 67 JNI function implementations

## Building

### Method 1: Automated Build (Recommended)

```bash
# Set JAVA_HOME if needed
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

# Run build script
./build.sh
```

### Method 2: Manual Build

```bash
gcc -shared -fPIC -o libaskar_minimal_test.so askar_jni_wrapper.c \
    -I$JAVA_HOME/include \
    -I$JAVA_HOME/include/linux \
    -lpthread
```

## Architecture

```
Java Application
       ↓ (JNI calls)
AskarNative.java
       ↓ (native methods)
askar_jni_wrapper.c
       ↓ (FFI calls)
Askar Core Library (Rust)
```

## Key Features

### Complete Implementation
- ✅ All store operations (provision, open, close, rekey)
- ✅ All session operations (start, update, fetch, count)
- ✅ All key operations (generate, sign, verify, derive)
- ✅ Advanced operations (scan, entry lists, profiles)

### Production Quality
- ✅ Thread-safe callback system
- ✅ Proper memory management
- ✅ Comprehensive error handling
- ✅ No external dependencies (only pthread)

### Performance
- ✅ Direct FFI calls to Rust
- ✅ Minimal overhead
- ✅ Efficient callback synchronization

## Usage

After building, the library is automatically copied to the resources directory and can be used by the Java wrapper:

```java
// In AskarNative.java
static {
    System.loadLibrary("askar_minimal_test");  // Loads libaskar_minimal_test.so
}
```

## Troubleshooting

### Build Issues

1. **`jni.h` not found**
   ```bash
   # Set JAVA_HOME correctly
   export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
   ```

2. **Permission denied**
   ```bash
   chmod +x build.sh
   ```

3. **GCC not found**
   ```bash
   sudo apt install build-essential
   ```

### Runtime Issues

1. **`UnsatisfiedLinkError`**
   - Ensure library path is correct: `-Djava.library.path=/path/to/native`
   - Check library file exists: `ls libaskar_minimal_test.so`

2. **Symbol lookup errors**
   - Library may need linking with Askar core
   - Check if Askar core library is available

## Development

### Adding New Functions

1. Add native method declaration to `AskarNative.java`
2. Implement JNI function in `askar_jni_wrapper.c`
3. Follow existing callback patterns
4. Rebuild with `./build.sh`
5. Test thoroughly

### Code Style

- Use consistent naming: `Java_org_hyperledger_aries_askar_AskarNative_functionName`
- Add debug printf statements for troubleshooting
- Handle all error cases with proper cleanup
- Follow existing callback and memory management patterns

## File History

- **Original**: `askar_minimal_test.c` (misleading name)
- **Renamed**: `askar_jni_wrapper.c` (accurate name reflecting its role)
- **Cleanup**: Removed CMake files (using direct GCC compilation)
- **Enhanced**: Added automated build script