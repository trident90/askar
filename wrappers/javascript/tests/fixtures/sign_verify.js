const path = require('path');
process.env.ASKAR_LIB_PATH = path.join(__dirname, '..', '..', '..', 'target', 'release', process.platform === 'win32' ? 'aries_askar.dll' : process.platform === 'darwin' ? 'libaries_askar.dylib' : 'libaries_askar.so');
const { LocalKey, KeyAlg } = require('../../lib');
(async () => {
  const key = await LocalKey.generate(KeyAlg.Ed25519);
  const msg = Buffer.from('hello');
  const sig = key.signMessage(msg);
  const ok = key.verifySignature(msg, sig);
  key.free();
  if (ok) process.stdout.write('ok'); else process.exit(2);
})().catch(e => { console.error(e && e.stack || e); process.exit(1); });
