const path = require('path');
process.env.ASKAR_LIB_PATH = path.join(__dirname, '..', '..', '..', 'target', 'release', process.platform === 'win32' ? 'aries_askar.dll' : process.platform === 'darwin' ? 'libaries_askar.dylib' : 'libaries_askar.so');
const { askarLib, koffi, SecretBuffer } = require('../../lib/ffi');
(async () => {
  // provision
  const outStore = koffi.alloc('size_t', 1);
  // Use KDF method with passphrase to create a valid key
  let rc = askarLib.askar_store_provision_sync('sqlite://:memory:', 'kdf:argon2i:int', 'test-key', null, 0, outStore);
  if (rc !== 0) throw new Error('provision rc=' + rc);
  const store = koffi.decode(outStore, 'size_t');

  // start session
  const outSess = koffi.alloc('size_t', 1);
  rc = askarLib.askar_session_start_sync(store, null, 0, outSess);
  if (rc !== 0) throw new Error('session start rc=' + rc);
  const sess = koffi.decode(outSess, 'size_t');

  // insert
  const bb = { len: Buffer.from('data-1').length, data: Buffer.from('data-1') };
  rc = askarLib.askar_session_update_sync(sess, 0, 'cats', 'name1', bb, JSON.stringify({ t: 'v' }), -1);
  if (rc !== 0) throw new Error('update rc=' + rc);

  // fetch
  const outList = koffi.alloc('size_t', 1);
  rc = askarLib.askar_session_fetch_sync(sess, 'cats', 'name1', 0, outList);
  if (rc !== 0) throw new Error('fetch rc=' + rc);
  const list = koffi.decode(outList, 'size_t');
  const countPtr = koffi.alloc('int32', 1);
  rc = askarLib.askar_entry_list_count(list, countPtr);
  if (rc !== 0) throw new Error('count rc=' + rc);
  const count = koffi.decode(countPtr, 'int32');
  let ok = false;
  if (count > 0) {
    const valPtr = koffi.alloc(SecretBuffer, 1);
    rc = askarLib.askar_entry_list_get_value(list, 0, valPtr);
    if (rc === 0) {
      const sb = koffi.decode(valPtr, SecretBuffer);
      const data = Buffer.from(koffi.decode(sb.data, koffi.array('uint8', sb.len)));
      ok = data.toString() === 'data-1';
      askarLib.askar_buffer_free(sb);
    }
  }
  askarLib.askar_entry_list_free(list);

  // close
  askarLib.askar_session_close_sync(sess, 1);
  askarLib.askar_store_close_sync(store);
  process.stdout.write(ok ? 'ok' : '');
})().catch(e => { console.error(e && e.stack || e); process.exit(1); });
