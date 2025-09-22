const { Store, LocalKey, KeyAlg, Askar } = require('aries-askar');

async function advancedExample() {
  try {
    console.log('Aries Askar Advanced Example');

    // Set up logging
    Askar.setDefaultLogger();

    // Create a SQLite file-based store (KDF recommended)
    const storeUri = 'sqlite://test-store.db';
    let store;

    try {
      // Try to open existing store
      store = await Store.open(storeUri, 'kdf:argon2i:int', 'test-key');
      console.log('Opened existing store');
    } catch (error) {
      // Create new store if it doesn't exist
      store = await Store.provision(storeUri, 'kdf:argon2i:int', 'test-key', null, true);
      console.log('Created new store');
    }

    // Create multiple profiles using safe path
    await store.createProfileNoPtr('profile1');
    await store.createProfileNoPtr('profile2');
    console.log('Ensured profiles: profile1, profile2');

    // List all profiles
    const profiles = await store.listProfiles();
    console.log('Available profiles:', profiles);

    // Work with different profiles (sync sessions)
    const session1 = store.startSessionSync('profile1');
    const session2 = store.startSessionSync('profile2');

    // Add data to different profiles (sync)
    session1.insertSync('users', 'alice', Buffer.from(JSON.stringify({ name: 'Alice' })), { role: 'admin', active: 'true' });
    session2.insertSync('users', 'bob', Buffer.from(JSON.stringify({ name: 'Bob' })), { role: 'user', active: 'true' });

    // Query data with filters (sync)
    const activeUsers = session1.fetchAllSync('users', { active: 'true' });
    console.log('Active users in profile1:', activeUsers.length);

    // Generate different types of keys
    const ed25519Key = LocalKey.generate(KeyAlg.Ed25519);
    const x25519Key = LocalKey.generate(KeyAlg.X25519);
    const p256Key = LocalKey.generate(KeyAlg.P256);

    console.log('Generated keys:');
    console.log('- Ed25519:', ed25519Key.getJwkThumbprint());
    console.log('- X25519:', x25519Key.getJwkThumbprint());
    console.log('- P-256:', p256Key.getJwkThumbprint());

    // (Omit storing keys in DB to avoid pointer-heavy paths in examples)

    // Demonstrate key exchange
    const alice_ephemeral = LocalKey.generate(KeyAlg.X25519, undefined, true);
    const bob_static = LocalKey.generate(KeyAlg.X25519);

    // ECDH-ES key derivation
    const sharedKey = LocalKey.deriveEcdhEs(
      KeyAlg.A256Gcm,
      alice_ephemeral,
      bob_static,
      'A256GCM',
      'Alice',
      'Bob',
      false
    );

    console.log('Derived shared key algorithm:', sharedKey.getAlgorithm());

    // Encrypt with derived key
    const message = 'This is a secret message';
    const encrypted = sharedKey.aeadEncrypt(message);
    console.log('Encrypted message length:', encrypted.ciphertext.length);

    // Decrypt the message
    const decrypted = sharedKey.aeadDecrypt(
      encrypted.ciphertext,
      encrypted.nonce,
      encrypted.tag
    );
    console.log('Decrypted message:', decrypted.toString());

    // (Omit scanning in the example to keep it concise and robust)

    // Demonstrate transactions
    console.log('\nDemonstrating transactions...');
    const transaction = store.startSessionSync('profile1', true);

    try {
      transaction.insertSync('temp', 'item1', Buffer.from('data1'));
      transaction.insertSync('temp', 'item2', Buffer.from('data2'));

      // Count items before commit
      const countBefore = transaction.countSync('temp');
      console.log('Items in transaction:', countBefore);

      // Commit the transaction
      transaction.closeSync(true);

      // Verify items are persisted
      const verifySession = store.startSessionSync('profile1');
      const countAfter = verifySession.countSync('temp');
      console.log('Items after commit:', countAfter);
      verifySession.closeSync();

    } catch (error) {
      // Rollback on error
      try { transaction.closeSync(false); } catch {}
      throw error;
    }

    // Clean up sessions
    session1.closeSync();
    session2.closeSync();

    // Copy store to a new location
    console.log('\nCopying store...');
    // (Omit store copy to keep the example focused)

    store.closeSync();
    console.log('Advanced example completed successfully');

  } catch (error) {
    console.error('Error in advanced example:', error);
    process.exit(1);
  } finally {
    // Package registers process-exit cleanup; avoid double termination here.
  }
}

if (require.main === module) {
  const keepAlive = setInterval(() => {}, 1000);
  advancedExample()
    .catch((e) => { console.error(e && e.stack || e); process.exitCode = 1; })
    .finally(() => { clearInterval(keepAlive); });
}

module.exports = { advancedExample };
