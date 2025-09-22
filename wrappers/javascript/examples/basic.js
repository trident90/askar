const { Store, LocalKey, KeyAlg, Askar } = require('aries-askar');
// Avoid double termination: remove package's auto-cleanup for this demo run
try {
  const utils = require('aries-askar/lib/utils');
  process.removeListener('exit', utils.cleanup);
  process.removeListener('SIGINT', utils.cleanup);
  process.removeListener('SIGTERM', utils.cleanup);
} catch {}

async function basicExample() {
  try {
    console.log('Aries Askar JavaScript Wrapper Example');
    console.log('Version:', Askar.getVersion());

    // Set up logging
    Askar.setDefaultLogger();

    // Create a test store (KDF recommended for in-memory)
    const storeUri = 'sqlite://:memory:';
    const store = await Store.provision(storeUri, 'kdf:argon2i:int', 'test-key');
    console.log('Store provisioned successfully');

    // Start a session (sync path for stability)
    const session = store.startSessionSync();
    console.log('Session started');

    // Insert some test data (sync)
    session.insertSync('category1', 'test-entry', Buffer.from('test data'), {
      tag1: 'value1',
      tag2: 'value2'
    });
    console.log('Data inserted');

    // Verify via count (sync)
    const count = session.countSync('category1');
    console.log('Entries in category1:', count);

    // Clean up (sync)
    session.closeSync(true);
    store.closeSync();
    console.log('Example completed successfully');

  } catch (error) {
    console.error('Error:', error);
    process.exit(1);
  } finally {
    // Package registers process-exit cleanup; avoid double termination here.
  }
}

if (require.main === module) {
  // Keep the event loop alive until the async work completes
  const keepAlive = setInterval(() => {}, 1000);
  basicExample()
    .catch((e) => { console.error(e && e.stack || e); process.exitCode = 1; })
    .finally(() => { clearInterval(keepAlive); });
}

module.exports = { basicExample };
