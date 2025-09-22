const path = require('path');
process.env.ASKAR_LIB_PATH = path.join(__dirname, '..', '..', '..', 'target', 'release', process.platform === 'win32' ? 'aries_askar.dll' : process.platform === 'darwin' ? 'libaries_askar.dylib' : 'libaries_askar.so');
const { LocalKey, KeyAlg } = require('../../lib');
(async () => {
  const key = await LocalKey.generate(KeyAlg.A256Gcm);
  const msg = Buffer.from('secret');
  const nonce = key.aeadRandomNonce();
  const enc = key.aeadEncrypt(msg, nonce, '');
  const tag = enc.ciphertext.slice(enc.tagPos, enc.tagPos + 16); // GCM tag 16 bytes
  const dec = key.aeadDecrypt(enc.ciphertext.slice(0, enc.tagPos), nonce, tag, '');
  const ok = dec.toString() === 'secret';
  key.free();
  process.stdout.write(ok ? 'ok' : '');
})().catch(e => { console.error(e && e.stack || e); process.exit(1); });

