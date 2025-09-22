# Aries Askar JavaScript Wrapper – User Manual

This guide explains how to install, build, and use the Aries Askar JavaScript wrapper (Node.js) with practical, copy‑paste examples. It focuses on safe, stable usage patterns for Node.js (incl. Node 20+).

## Overview

- Native library: Rust (aries-askar) built with SQLite feature
- JS wrapper: Node.js + Koffi FFI (no native compilation for JS)
- Supports stores, sessions, profiles, keys, AEAD, signatures, scans

## Prerequisites

- Node.js 16+ (tested on 16/18/20+)
- Rust (to build the native library)

## Install and Build

1) Build the native library with SQLite enabled:

```bash
# from the repository root
cargo build --release --features sqlite
```

2) Install JS wrapper dependencies and build TypeScript:

```bash
cd wrappers/javascript
npm install
npm run build
```

3) Ensure Node can locate the native library. The wrapper tries several paths automatically. If needed, set the environment variable:

```bash
export ASKAR_LIB_PATH="$(pwd)/../../target/release/libaries_askar.so"  # Linux
# export ASKAR_LIB_PATH="$(pwd)/../../target/release/libaries_askar.dylib"  # macOS
# setx ASKAR_LIB_PATH C:\\path\\to\\aries_askar.dll  # Windows (PowerShell: [Environment]::SetEnvironmentVariable)
```

## Quick Start

```javascript
const { Store, LocalKey, KeyAlg, Askar } = require('aries-askar');

(async () => {
  Askar.setDefaultLogger();

  // Prefer KDF-based method for in-memory stores to avoid rc=5 errors
  const store = await Store.provision('sqlite://:memory:', 'kdf:argon2i:int', 'passphrase');

  const session = await store.startSession();

  // Insert some data
  await session.insert('cats', 'tom', Buffer.from('meow'), { color: 'grey' });

  // Fetch it back
  const entry = await session.fetch('cats', 'tom');
  console.log('Value:', entry.value.toString());

  // Generate a key and store it
  const key = LocalKey.generate(KeyAlg.Ed25519);
  await session.insertKey(key, 'signing-key', 'metadata', { purpose: 'signing' });

  // Sign/verify
  const sig = key.signMessage('hello');
  console.log('Signature valid?', key.verifySignature('hello', sig));

  key.free();
  await session.close(true);
  await store.close();
})();
```

## Safe Patterns (Recommended)

Some Node environments are sensitive to char** pointer marshalling. Use these helpers to avoid fragile paths:

```javascript
const { Store } = require('aries-askar');

(async () => {
  const store = await Store.provision('sqlite://:memory:', 'kdf:argon2i:int', 'pass');

  // Create profiles without returning name pointer
  await store.createProfileNoPtr('p1');
  await store.createProfileNoPtr('p2');

  // Check existence via boolean
  console.log('p1 exists?', await store.profileExists('p1'));

  // Remove a profile synchronously (returns boolean)
  console.log('Removed p1?', store.removeProfileSync('p1'));

  // Close synchronously
  store.closeSync();
})();
```

If you provision via sync FFI elsewhere and only have a native handle, you can still wrap it:

```javascript
const { Store } = require('aries-askar');
// const handle = ... // from askar_store_provision_sync
const store = Store.fromHandle(handle);
```

## Provisioning and Opening

- Provision a new store (creates DB if needed):

```javascript
const store = await Store.provision('sqlite://:memory:', 'kdf:argon2i:int', 'passphrase');
```

- Open an existing store:

```javascript
const store = await Store.open('sqlite://path/to.db', 'kdf:argon2i:int', 'passphrase');
```

- Remove a store (by URI):

```javascript
const removed = await Store.remove('sqlite://path/to.db');
```

## Profiles

```javascript
const store = await Store.provision('sqlite://:memory:', 'kdf:argon2i:int', 'pass');

// Create
await store.createProfileNoPtr('team-a');

// Exists?
if (await store.profileExists('team-a')) {
  console.log('team-a available');
}

// List (safe JSON path is used internally when available)
const profiles = await store.listProfiles();
console.log('profiles:', profiles);

// Remove
const removed = store.removeProfileSync('team-a');
console.log('removed team-a?', removed);

store.closeSync();
```

## Sessions and Entries

```javascript
const store = await Store.provision('sqlite://:memory:', 'kdf:argon2i:int', 'pass');
const session = await store.startSession();

// Insert / Replace / Remove entries
await session.insert('docs', 'readme', Buffer.from('Hello'), { lang: 'en' });
await session.replace('docs', 'readme', Buffer.from('Hello, world'), { lang: 'en' });
const doc = await session.fetch('docs', 'readme');
console.log(doc.value.toString());

// Fetch all (optionally with tag filters)
const all = await session.fetchAll('docs');
console.log('docs count:', all.length);

await session.remove('docs', 'readme');
await session.close(true);
await store.close();
```

## Keys, AEAD, Sign/Verify

```javascript
const { LocalKey, KeyAlg } = require('aries-askar');

// Key generation
const key = LocalKey.generate(KeyAlg.Ed25519);

// Sign/verify
const sig = key.signMessage('lorem');
console.log('valid?', key.verifySignature('lorem', sig));

// AEAD example (with a symmetric key algorithm)
// const enc = key.aeadEncrypt(message, nonce, aad);
// const dec = key.aeadDecrypt(ciphertext, nonce, tag, aad);

key.free();
```

## Scans (Iterating Large Result Sets)

```javascript
const { Scan } = require('aries-askar');

const scan = await store.startScan(/* profile */ undefined, /* category */ 'cats');
let batch;
while ((batch = await scan.next()).length > 0) {
  for (const entry of batch) {
    console.log(entry.category, entry.name);
  }
}
scan.free();
```

## Advanced Example (Sync paths for stability)

```javascript
const { Store, LocalKey, KeyAlg } = require('aries-askar');

(async () => {
  const store = await Store.provision('sqlite://test-store.db', 'kdf:argon2i:int', 'pass', null, true);
  await store.createProfileNoPtr('profile1');
  const session = store.startSessionSync('profile1');
  session.insertSync('users', 'alice', Buffer.from('Alice'), { role: 'admin' });
  const count = session.countSync('users');
  console.log('users count:', count);
  session.closeSync(true);
  store.closeSync();
})();
```

Notes:
- Prefer sync helpers in examples/scripts to avoid pointer-heavy callbacks in certain Node environments.
- Ensure the native library version matches the npm package version. Use `ASKAR_LIB_PATH` to point to the correct library.

## Store/Session (npm) — Minimal Safe Flow

```javascript
const { Store } = require('aries-askar');

(async () => {
  const store = await Store.provision('sqlite://:memory:', 'kdf:argon2i:int', 'pass');
  const session = store.startSessionSync();
  session.insertSync('demo', 'e1', Buffer.from('v1'));
  console.log('count:', session.countSync('demo'));
  session.closeSync(true);
  store.closeSync();
})();
```

## Advanced Store/Session (npm) — Transactions & Updates

```javascript
const { Store } = require('aries-askar');

(async () => {
  const store = await Store.provision('sqlite://:memory:', 'kdf:argon2i:int', 'pass');
  const tx = store.startSessionSync(undefined, true);
  tx.insertSync('orders', 'o1', Buffer.from('A'), { status: 'open' });
  tx.insertSync('orders', 'o2', Buffer.from('B'), { status: 'open' });
  tx.insertSync('orders', 'o3', Buffer.from('C'), { status: 'closed' });
  console.log('inTx:', tx.countSync('orders'));
  tx.closeSync(true);
  const s = store.startSessionSync();
  console.log('total:', s.countSync('orders'));
  s.replaceSync('orders', 'o2', Buffer.from('B2'), { status: 'closed' });
  s.removeSync('orders', 'o1');
  console.log('after:', s.countSync('orders'));
  s.closeSync();
  store.closeSync();
})();
```

## Profiles & Scan (npm) — Safe Helpers + Counts

```javascript
const { Store } = require('aries-askar');

(async () => {
  const store = await Store.provision('sqlite://:memory:', 'kdf:argon2i:int', 'pass');
  const session = store.startSessionSync();
  session.insertSync('cats', 'tom', Buffer.from('meow'), { color: 'grey' });
  session.insertSync('cats', 'jerry', Buffer.from('squeak'), { color: 'brown' });
  session.insertSync('dogs', 'pluto', Buffer.from('woof'), { color: 'yellow' });
  console.log('cats:', session.countSync('cats'), 'all:', session.countSync());
  await store.createProfileNoPtr('p1');
  await store.createProfileNoPtr('p2');
  // Optional JSON-sync listing (set ENABLE_LIST=1) with stabilization & retries.
  // listProfilesStable(retries=2, delayMs=30) → tries JSON path, falls back automatically.
  if (process.env.ENABLE_LIST === '1') {
    console.log('list(stable):', await store.listProfilesStable(2, 30));
  }
  console.log('exists:', await store.profileExists('p1'), await store.profileExists('p2'));
  session.closeSync(true);
  store.closeSync();
})();
```

Notes:
- Prefer counts to emulate scans (`fetchAllSync` may be pointer-heavy in some environments).
- Use `createProfileNoPtr` and `profileExists` to avoid char** returns.

## Error Codes and Troubleshooting

- Common rc values (ErrorCode):
  - 0: Success
  - 5: Input (e.g., invalid parameters, missing key)
  - 8: Unsupported
- For in-memory SQLite (`sqlite://:memory:`), avoid `raw` with blank pass key. Prefer `kdf:argon2i:int` + passphrase.
- Node char** pointer marshalling can be fragile. Prefer the safe helpers:
  - `createProfileNoPtr`, `profileExists`, `removeProfileSync`, `closeSync`.
- Ensure the native library can be found (set `ASKAR_LIB_PATH` if needed).
- Terminate library (advanced): `Askar.terminate()` when you need to force cleanup.

## Testing Locally

```bash
# From repo root (build library)
cargo build --release --features sqlite

# JS wrapper (install + build)
cd wrappers/javascript
npm install
npm run build

# Run built-in Node tests
node scripts/test-node.js
```

## Notes

- The wrapper targets stability and safety by avoiding fragile pointer patterns when possible.
- For production, prefer file-backed SQLite (e.g., `sqlite://data/askar.db`) over in-memory.

---
If you find issues or have feature requests, please open an issue or PR in this repository.
## File-based Variants

The same stable patterns apply to file-backed SQLite stores:

```javascript
const { Store } = require('aries-askar');

// Minimal file-based flow
(async () => {
  const uri = 'sqlite://file-demo.db';
  const store = await Store.provision(uri, 'kdf:argon2i:int', 'file-pass', null, true);
  const s = store.startSessionSync();
  s.insertSync('demo', 'file', Buffer.from('v'), { t: 'f' });
  console.log('count(file):', s.countSync('demo'));
  s.closeSync(true);
  store.closeSync();
})();

// Advanced file-based flow
(async () => {
  const uri = 'sqlite://file-adv-demo.db';
  const store = await Store.provision(uri, 'kdf:argon2i:int', 'file-adv-pass', null, true);
  const tx = store.startSessionSync(undefined, true);
  tx.insertSync('orders', 'o1', Buffer.from('A'), { status: 'open' });
  tx.insertSync('orders', 'o2', Buffer.from('B'), { status: 'open' });
  console.log('inTx(file):', tx.countSync('orders'));
  tx.closeSync(true);
  const s = store.startSessionSync();
  s.replaceSync('orders', 'o2', Buffer.from('B2'), { status: 'closed' });
  console.log('after(file):', s.countSync('orders'));
  s.closeSync();
  store.closeSync();
})();
```

Notes:
- Use `recreate=true` for a clean file during demos/tests.
- Keep using sync helpers to avoid callback/pointer issues in certain runtimes.
