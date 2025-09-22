const { LocalKey, KeyAlg, Askar } = require('aries-askar');
// Avoid auto-cleanup conflicts in this demo
try {
  const utils = require('aries-askar/lib/utils');
  process.removeListener('exit', utils.cleanup);
  process.removeListener('SIGINT', utils.cleanup);
  process.removeListener('SIGTERM', utils.cleanup);
} catch {}

(async () => {
  try {
    console.log('Aries Askar Advanced Example');
    try { console.log('Version:', Askar.getVersion()); } catch {}

    // Generate different types of keys (no store/session required)
    const ed25519Key = await LocalKey.generate(KeyAlg.Ed25519);
    const x25519Key = await LocalKey.generate(KeyAlg.X25519);
    const p256Key = await LocalKey.generate(KeyAlg.P256);

    console.log('Generated keys:');
    console.log('- Ed25519 alg:', ed25519Key.getAlgorithm ? ed25519Key.getAlgorithm() : 'ed25519');
    console.log('- X25519 alg:', x25519Key.getAlgorithm ? x25519Key.getAlgorithm() : 'x25519');
    console.log('- P-256 alg:', p256Key.getAlgorithm ? p256Key.getAlgorithm() : 'p256');

    // Signing (Ed25519)
    const msg = Buffer.from('advanced-demo');
    const sig = ed25519Key.signMessage(msg);
    const ok = ed25519Key.verifySignature(msg, sig);
    console.log('Signature ok?', !!ok);

    console.log('Advanced example completed successfully');
  } catch (e) {
    console.error('Error in advanced example:', e && e.stack || e);
    process.exit(1);
  }
})();
