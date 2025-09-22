const { Store, Askar } = require('aries-askar');

async function main() {
  try {
    console.log('Aries Askar Store/Session (npm) Example');
    try { console.log('Version:', Askar.getVersion()); } catch {}

    // Provision in-memory SQLite store (use KDF to avoid rc=5)
    const store = await Store.provision('sqlite://:memory:', 'kdf:argon2i:int', 'test-key');
    console.log('Store provisioned');

    // Start session (sync for stability)
    const session = store.startSessionSync();
    console.log('Session started');

    // Insert and verify via count (sync)
    session.insertSync('demo', 'entry', Buffer.from('value'), { t: 'v' });
    const count = session.countSync('demo');
    console.log('Count in category demo:', count);

    // Close (sync)
    session.closeSync(true);
    store.closeSync();
    console.log('Done');
  } catch (e) {
    console.error(e && e.stack || e);
    process.exit(1);
  }
}

if (require.main === module) {
  const keepAlive = setInterval(() => {}, 1000);
  main()
    .catch((e) => { console.error(e && e.stack || e); process.exitCode = 1; })
    .finally(() => { clearInterval(keepAlive); });
}

module.exports = { main };

