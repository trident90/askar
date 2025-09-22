const { Store, Askar } = require('aries-askar');

async function main() {
  try {
    console.log('Aries Askar Advanced Store/Session (npm) Example');
    try { console.log('Version:', Askar.getVersion()); } catch {}

    // Provision in-memory store using KDF (stable in npm)
    const store = await Store.provision('sqlite://:memory:', 'kdf:argon2i:int', 'adv-pass');
    console.log('Store provisioned');

    // Start a transaction (sync) in the default profile
    const tx = store.startSessionSync(undefined, true);
    console.log('Transaction started');

    // Insert a small batch (sync)
    tx.insertSync('orders', 'order-001', Buffer.from('A'), { status: 'open' });
    tx.insertSync('orders', 'order-002', Buffer.from('B'), { status: 'open' });
    tx.insertSync('orders', 'order-003', Buffer.from('C'), { status: 'closed' });

    // Count before commit (sync)
    const inTx = tx.countSync('orders');
    console.log('Orders in transaction:', inTx);

    // Commit (sync)
    tx.closeSync(true);
    console.log('Transaction committed');

    // Verify persistence (sync)
    const verify = store.startSessionSync();
    const total = verify.countSync('orders');
    const open = verify.countSync('orders', { status: 'open' });
    const closed = verify.countSync('orders', { status: 'closed' });
    console.log('Orders persisted - total:', total, 'open:', open, 'closed:', closed);

    // Replace + remove (sync)
    verify.replaceSync('orders', 'order-002', Buffer.from('B2'), { status: 'closed' });
    verify.removeSync('orders', 'order-001');

    const after = verify.countSync('orders');
    const closed2 = verify.countSync('orders', { status: 'closed' });
    console.log('After updates - total:', after, 'closed:', closed2);

    verify.closeSync();
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

