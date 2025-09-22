const path = require('path');
process.env.ASKAR_LIB_PATH = path.join(__dirname, '..', '..', '..', 'target', 'release', process.platform === 'win32' ? 'aries_askar.dll' : process.platform === 'darwin' ? 'libaries_askar.dylib' : 'libaries_askar.so');
const { LocalKey, KeyAlg } = require('../../lib');
(async () => {
  const recip = await LocalKey.generate(KeyAlg.X25519);
  const sender = await LocalKey.generate(KeyAlg.X25519);
  const msg = Buffer.from('hello');
  const nonce = (await LocalKey.cryptoBoxRandomNonce());
  const ct = recip.cryptoBox(sender, msg, nonce);
  const pt = recip.cryptoBoxOpen(sender, ct, nonce);
  const ok = pt.toString() === 'hello';
  recip.free(); sender.free();
  process.stdout.write(ok ? 'ok' : '');
})().catch(e => { console.error(e && e.stack || e); process.exit(1); });

