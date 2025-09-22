const { Store, Askar } = require('aries-askar');

async function main() {
  try {
    console.log('Aries Askar Profiles List (npm) Example');
    try { console.log('Version:', Askar.getVersion()); } catch {}

    const store = await Store.provision('sqlite://:memory:', 'kdf:argon2i:int', 'list-pass');
    await store.createProfileNoPtr('L1');
    await store.createProfileNoPtr('L2');

    // Attempt JSON-safe listing
    const profiles = await store.listProfiles();
    console.log('profiles:', profiles);

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

