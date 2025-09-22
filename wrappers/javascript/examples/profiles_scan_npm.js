const { Store, Askar } = require('aries-askar');

async function main() {
  try {
    console.log('Aries Askar Profiles & Scan (npm) Example');
    try { console.log('Version:', Askar.getVersion()); } catch {}

    // Provision in-memory store with KDF
    const store = await Store.provision('sqlite://:memory:', 'kdf:argon2i:int', 'profiles-pass');
    console.log('Store provisioned');

    // Use default profile for session (sync)
    const session = store.startSessionSync();

    // Insert entries (sync)
    session.insertSync('cats', 'tom', Buffer.from('meow'), { color: 'grey' });
    session.insertSync('cats', 'jerry', Buffer.from('squeak'), { color: 'brown' });
    session.insertSync('dogs', 'pluto', Buffer.from('woof'), { color: 'yellow' });

    // Count (sync) to emulate a scan, avoid pointer-heavy fetchAll
    const catsCount = session.countSync('cats');
    const allCount = session.countSync();
    console.log('cats:', catsCount, 'all:', allCount);

    // Finish session before profile ops
    session.closeSync(true);

    // Create profiles using safe paths
    await store.createProfileNoPtr('p1');
    await store.createProfileNoPtr('p2');
    // Optional: list profiles via JSON-sync with stabilization (ENABLE_LIST=1)
    if (process.env.ENABLE_LIST === '1') {
      const profiles = await store.listProfilesStable(2, 30);
      console.log('profiles(stable):', profiles);
    }
    console.log('exists:', await store.profileExists('p1'), await store.profileExists('p2'));

    // Clean up
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
