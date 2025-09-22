const path = require('path');
process.env.ASKAR_LIB_PATH = path.join(__dirname, '..', '..', '..', 'target', 'release', process.platform === 'win32' ? 'aries_askar.dll' : process.platform === 'darwin' ? 'libaries_askar.dylib' : 'libaries_askar.so');
const { askarLib, koffi } = require('../../lib/ffi');
(async () => {
  // Provision store
  const outStore = koffi.alloc('size_t', 1);
  let rc = askarLib.askar_store_provision_sync('sqlite://:memory:', 'kdf:argon2i:int', 'test-key', null, 0, outStore);
  if (rc !== 0) throw new Error('provision rc=' + rc);
  const store = koffi.decode(outStore, 'size_t');

  // Create profiles without name out param
  rc = askarLib.askar_store_create_profile_noptr_sync(store, 'p1');
  if (rc !== 0) throw new Error('create p1 rc=' + rc);
  rc = askarLib.askar_store_create_profile_noptr_sync(store, 'p2');
  if (rc !== 0) throw new Error('create p2 rc=' + rc);

  // Validate existence of created profiles using a safe bool check
  const existsPtr1 = koffi.alloc('int8', 1);
  rc = askarLib.askar_store_profile_exists_sync(store, 'p1', existsPtr1);
  if (rc !== 0) throw new Error('exists p1 rc=' + rc);
  const p1exists = koffi.decode(existsPtr1, 'int8') !== 0;
  const existsPtr2 = koffi.alloc('int8', 1);
  rc = askarLib.askar_store_profile_exists_sync(store, 'p2', existsPtr2);
  if (rc !== 0) throw new Error('exists p2 rc=' + rc);
  const p2exists = koffi.decode(existsPtr2, 'int8') !== 0;

  // Remove one profile and validate states again
  const removedPtr = koffi.alloc('int8', 1);
  rc = askarLib.askar_store_remove_profile_sync(store, 'p1', removedPtr);
  const removed = koffi.decode(removedPtr, 'int8') !== 0;

  const existsPtr1b = koffi.alloc('int8', 1);
  rc = askarLib.askar_store_profile_exists_sync(store, 'p1', existsPtr1b);
  const p1existsAfter = koffi.decode(existsPtr1b, 'int8') !== 0;
  const existsPtr2b = koffi.alloc('int8', 1);
  rc = askarLib.askar_store_profile_exists_sync(store, 'p2', existsPtr2b);
  const p2existsAfter = koffi.decode(existsPtr2b, 'int8') !== 0;

  const ok = p1exists && p2exists && removed && !p1existsAfter && p2existsAfter;
  process.stdout.write(ok ? 'ok' : '');
})().catch(e => { console.error(e && e.stack || e); process.exit(1); });
