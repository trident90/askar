const { Store, Askar } = require('aries-askar');

async function main() {
  try {
    console.log('Aries Askar Profiles & Scan (file, npm) Example');
    try { console.log('Version:', Askar.getVersion()); } catch {}

    const uri = 'sqlite://file-profiles-demo.db';
    const store = await Store.provision(uri, 'kdf:argon2i:int', 'prof-pass', null, true);
    console.log('Store provisioned at', uri);

    // Session ops (sync)
    const session = store.startSessionSync();
    session.insertSync('cats', 'felix', Buffer.from('meow'), { color: 'black' });
    session.insertSync('dogs', 'max', Buffer.from('woof'), { color: 'brown' });
    console.log('counts:', session.countSync('cats'), session.countSync());
    session.closeSync(true);

    // Profiles
    await store.createProfileNoPtr('pf1');
    await store.createProfileNoPtr('pf2');
    if (process.env.ENABLE_LIST === '1') {
      console.log('profiles(stable):', await store.listProfilesStable(2, 30));
    }
    console.log('exists:', await store.profileExists('pf1'), await store.profileExists('pf2'));

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

