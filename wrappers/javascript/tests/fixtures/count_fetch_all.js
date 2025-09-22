const path = require('path');
process.env.ASKAR_LIB_PATH = path.join(__dirname, '..', '..', '..', 'target', 'release', process.platform === 'win32' ? 'aries_askar.dll' : process.platform === 'darwin' ? 'libaries_askar.dylib' : 'libaries_askar.so');
const { askarLib, koffi, SecretBuffer } = require('../../lib/ffi');
(async () => {
  const outStore = koffi.alloc('size_t', 1);
  // Use KDF method with passphrase to create a valid key
  let rc = askarLib.askar_store_provision_sync('sqlite://:memory:', 'kdf:argon2i:int', 'test-key', null, 0, outStore);
  if (rc !== 0) throw new Error('provision rc=' + rc);
  const store = koffi.decode(outStore, 'size_t');
  const outSess = koffi.alloc('size_t', 1);
  rc = askarLib.askar_session_start_sync(store, null, 0, outSess);
  if (rc !== 0) throw new Error('session rc=' + rc);
  const sess = koffi.decode(outSess, 'size_t');
  // insert three
  const ins = (name, val, tag) => askarLib.askar_session_update_sync(sess, 0, 'cats', name, { len: Buffer.from(val).length, data: Buffer.from(val) }, JSON.stringify(tag), -1);
  ins('a', '1', { type: 'A' });
  ins('b', '2', { type: 'B' });
  ins('c', '3', { type: 'A' });
  // count
  const cptr = koffi.alloc('int64', 1);
  rc = askarLib.askar_session_count_sync(sess, 'cats', null, cptr);
  const count = koffi.decode(cptr, 'int64');
  // fetchAll all
  const outList = koffi.alloc('size_t', 1);
  rc = askarLib.askar_session_fetch_all_sync(sess, 'cats', null, -1, null, 0, 0, outList);
  const listAll = koffi.decode(outList, 'size_t');
  const cAllPtr = koffi.alloc('int32', 1);
  askarLib.askar_entry_list_count(listAll, cAllPtr);
  const lenAll = koffi.decode(cAllPtr, 'int32');
  askarLib.askar_entry_list_free(listAll);
  // fetchAll filtered
  const outList2 = koffi.alloc('size_t', 1);
  rc = askarLib.askar_session_fetch_all_sync(sess, 'cats', JSON.stringify({ type: 'A' }), -1, null, 0, 0, outList2);
  const listF = koffi.decode(outList2, 'size_t');
  const cFPtr = koffi.alloc('int32', 1);
  askarLib.askar_entry_list_count(listF, cFPtr);
  const lenF = koffi.decode(cFPtr, 'int32');
  askarLib.askar_entry_list_free(listF);
  // close
  askarLib.askar_session_close_sync(sess, 1);
  askarLib.askar_store_close_sync(store);
  const ok = count === 3 && lenAll === 3 && lenF === 2;
  process.stdout.write(ok ? 'ok' : '');
})().catch(e => { console.error(e && e.stack || e); process.exit(1); });
