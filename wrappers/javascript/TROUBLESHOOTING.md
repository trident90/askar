# Troubleshooting

## Node.js Compatibility Issues

### Problem: ffi-napi compilation errors with Node.js v20+

If you encounter compilation errors like:
```
error: invalid conversion from 'napi_finalize' to 'node_api_nogc_finalize'
```

This is due to Node.js v20+ API changes that `ffi-napi` doesn't support yet.

### Solutions:

#### Option 1: Use Koffi (Recommended)
We've updated the wrapper to use [Koffi](https://github.com/Koromix/koffi) instead of `ffi-napi`:

```bash
npm install koffi
```

Koffi is:
- ✅ Compatible with Node.js v20+
- ✅ Better performance
- ✅ Modern TypeScript support
- ✅ No native compilation required

#### Option 2: Downgrade Node.js
If you must use `ffi-napi`:

```bash
# Using nvm
nvm install 18
nvm use 18
npm install ffi-napi ref-napi ref-struct-di ref-array-di
```

#### Option 3: Use Alternative FFI Libraries
Other options include:
- `@napi-rs/ffi` (Rust-based)
- `node-ffi-rs` (Rust-based)

### Library Path Issues

If you get "library not found" errors:

1. **Build the Askar library first**:
   ```bash
   cd /path/to/askar
   cargo build --release --features ffi
   ```

2. **Set custom library path**:
   ```bash
   export ASKAR_LIB_PATH="/custom/path/to/libaries_askar.so"
   node your-script.js
   ```

3. **Or in your code**:
   ```javascript
   process.env.ASKAR_LIB_PATH = '/path/to/libaries_askar.so';
   const askar = require('aries-askar');
   ```

### Platform-specific Issues

#### Linux
- Install build essentials: `sudo apt-get install build-essential`
- Install Python dev headers: `sudo apt-get install python3-dev`

#### macOS
- Install Xcode command line tools: `xcode-select --install`
- For M1 Macs, ensure you're using the correct architecture

#### Windows
- Install Visual Studio Build Tools
- Use Developer Command Prompt

### Memory Issues

If you encounter memory leaks or crashes:

1. **Always free resources**:
   ```javascript
   key.free();
   await session.close();
   await store.close();
   ```

2. **Use proper error handling**:
   ```javascript
   try {
     const store = await Store.open(...);
     // ... work with store
   } catch (error) {
     console.error('Store error:', error);
   } finally {
     if (store) await store.close();
   }
   ```

3. **Avoid circular references**:
   ```javascript
   // Don't store FFI objects in circular structures
   ```

### Performance Optimization

1. **Reuse connections**:
   ```javascript
   // Don't create new stores for every operation
   const store = await Store.open(...);
   const session = await store.startSession();
   // Reuse session for multiple operations
   ```

2. **Use transactions for bulk operations**:
   ```javascript
   const transaction = await store.startSession(null, true);
   try {
     // Multiple operations
     await transaction.close(true); // commit
   } catch (error) {
     await transaction.close(false); // rollback
   }
   ```

3. **Use scanning for large datasets**:
   ```javascript
   const scan = await store.startScan();
   let batch;
   while ((batch = await scan.next()).length > 0) {
     // Process batch
   }
   scan.free();
   ```

## Common Error Codes

- `ErrorCode.Backend = 1`: Database/storage backend error
- `ErrorCode.Busy = 2`: Resource is busy (try again)
- `ErrorCode.Duplicate = 3`: Entry already exists
- `ErrorCode.Encryption = 4`: Cryptographic operation failed
- `ErrorCode.Input = 5`: Invalid input parameters
- `ErrorCode.NotFound = 6`: Entry not found
- `ErrorCode.Unexpected = 7`: Unexpected internal error
- `ErrorCode.Unsupported = 8`: Operation not supported

## Getting Help

1. Check the [examples](./examples/) directory
2. Review the [README](./README.md) for API documentation
3. File issues at the [Aries Askar repository](https://github.com/hyperledger/aries-askar/issues)