const path = require('path');
process.env.ASKAR_LIB_PATH = path.join(__dirname, '..', '..', '..', 'target', 'release', process.platform === 'win32' ? 'aries_askar.dll' : process.platform === 'darwin' ? 'libaries_askar.dylib' : 'libaries_askar.so');
const { Store } = require('../../lib');
const { askarLib, koffi } = require('../../lib/ffi');
(async () => {
  // Provision store via sync FFI, then wrap handle
  const outStore = koffi.alloc('size_t', 1);
  let rc = askarLib.askar_store_provision_sync('sqlite://:memory:', 'kdf:argon2i:int', 'test-key', null, 0, outStore);
  if (rc !== 0) throw new Error('provision rc=' + rc);
  const handle = koffi.decode(outStore, 'size_t');
  const store = Store.fromHandle(handle);

  // Create profiles (safe, no pointer handling)
  await store.createProfileNoPtr('p1');
  await store.createProfileNoPtr('p2');

  // Validate existence via bool helper
  const p1exists = await store.profileExists('p1');
  const p2exists = await store.profileExists('p2');

  // Remove one and validate states again
  const removed = store.removeProfileSync('p1');
  const p1existsAfter = await store.profileExists('p1');
  const p2existsAfter = await store.profileExists('p2');

  store.closeSync();
  const ok = p1exists && p2exists && removed && !p1existsAfter && p2existsAfter;
  process.stdout.write(ok ? 'ok' : '');
})().catch((e) => { console.error((e && e.stack) || e); process.exit(1); });
