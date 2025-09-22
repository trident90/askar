const { Store, Askar } = require('aries-askar');

async function main() {
  try {
    console.log('Aries Askar Advanced Store/Session (file, npm) Example');
    try { console.log('Version:', Askar.getVersion()); } catch {}

    // File-based SQLite, recreate=true for a clean slate
    const uri = 'sqlite://file-adv-demo.db';
    const store = await Store.provision(uri, 'kdf:argon2i:int', 'file-adv-pass', null, true);
    console.log('Store provisioned at', uri);

    // Transaction (sync)
    const tx = store.startSessionSync(undefined, true);
    tx.insertSync('orders', 'f-o1', Buffer.from('X'), { status: 'open' });
    tx.insertSync('orders', 'f-o2', Buffer.from('Y'), { status: 'open' });
    tx.insertSync('orders', 'f-o3', Buffer.from('Z'), { status: 'closed' });
    console.log('inTx:', tx.countSync('orders'));
    tx.closeSync(true);

    // Verify + update/remove (sync)
    const s = store.startSessionSync();
    console.log('total:', s.countSync('orders'));
    s.replaceSync('orders', 'f-o2', Buffer.from('Y2'), { status: 'closed' });
    s.removeSync('orders', 'f-o1');
    console.log('after:', s.countSync('orders'));
    s.closeSync();

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

