const { Store, Askar } = require('aries-askar');

async function main() {
  try {
    console.log('Aries Askar Store/Session (file, npm) Example');
    try { console.log('Version:', Askar.getVersion()); } catch {}

    // File-based SQLite store (KDF recommended). Use recreate=true to start fresh.
    const uri = 'sqlite://file-demo.db';
    const store = await Store.provision(uri, 'kdf:argon2i:int', 'file-pass', null, true);
    console.log('Store provisioned at', uri);

    // Start session (sync)
    const session = store.startSessionSync();
    console.log('Session started');

    // Insert/Count (sync)
    session.insertSync('demo', 'file-entry', Buffer.from('file-value'), { type: 'file' });
    console.log('Count in demo:', session.countSync('demo'));

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
  main().finally(() => clearInterval(keepAlive));
}

module.exports = { main };

