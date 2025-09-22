const { Store, LocalKey, KeyAlg, Askar } = require('../lib');

async function advancedExample() {
  try {
    console.log('Aries Askar Advanced Example');

    // Set up logging
    Askar.setDefaultLogger();

    // Create a SQLite file-based store
    const storeUri = 'sqlite://test-store.db';
    let store;

    try {
      // Try to open existing store
      store = await Store.open(storeUri, 'raw', 'test-key');
      console.log('Opened existing store');
    } catch (error) {
      // Create new store if it doesn't exist
      store = await Store.provision(storeUri, 'raw', 'test-key', null, true);
      console.log('Created new store');
    }

    // Create multiple profiles
    const profile1 = await store.createProfile('profile1');
    const profile2 = await store.createProfile('profile2');
    console.log('Created profiles:', profile1, profile2);

    // List all profiles
    const profiles = await store.listProfiles();
    console.log('Available profiles:', profiles);

    // Work with different profiles
    const session1 = await store.startSession('profile1');
    const session2 = await store.startSession('profile2');

    // Add data to different profiles
    await session1.insert('users', 'alice', Buffer.from(JSON.stringify({
      name: 'Alice',
      email: 'alice@example.com'
    })), { role: 'admin', active: 'true' });

    await session2.insert('users', 'bob', Buffer.from(JSON.stringify({
      name: 'Bob',
      email: 'bob@example.com'
    })), { role: 'user', active: 'true' });

    // Query data with filters
    const activeUsers = await session1.fetchAll('users', { active: 'true' });
    console.log('Active users in profile1:', activeUsers.length);

    // Generate different types of keys
    const ed25519Key = LocalKey.generate(KeyAlg.Ed25519);
    const x25519Key = LocalKey.generate(KeyAlg.X25519);
    const p256Key = LocalKey.generate(KeyAlg.P256);

    console.log('Generated keys:');
    console.log('- Ed25519:', ed25519Key.getJwkThumbprint());
    console.log('- X25519:', x25519Key.getJwkThumbprint());
    console.log('- P-256:', p256Key.getJwkThumbprint());

    // Store keys with metadata
    await session1.insertKey(ed25519Key, 'signing-key', JSON.stringify({
      purpose: 'authentication',
      created: new Date().toISOString()
    }), { type: 'signing', algorithm: 'Ed25519' });

    await session1.insertKey(x25519Key, 'exchange-key', JSON.stringify({
      purpose: 'key-agreement',
      created: new Date().toISOString()
    }), { type: 'exchange', algorithm: 'X25519' });

    // Fetch keys by algorithm
    const signingKeys = await session1.fetchAllKeys('ed25519');
    const exchangeKeys = await session1.fetchAllKeys('x25519');

    console.log('Signing keys:', signingKeys.map(k => k.name));
    console.log('Exchange keys:', exchangeKeys.map(k => k.name));

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

    // Demonstrate scanning
    console.log('\nScanning all entries...');
    const scan = await store.startScan('profile1');
    let batch;
    let totalEntries = 0;

    while ((batch = await scan.next()).length > 0) {
      totalEntries += batch.length;
      batch.forEach(entry => {
        console.log(`- ${entry.category}/${entry.name}: ${entry.value.length} bytes`);
      });
    }

    console.log(`Total entries scanned: ${totalEntries}`);
    scan.free();

    // Demonstrate transactions
    console.log('\nDemonstrating transactions...');
    const transaction = await store.startSession('profile1', true);

    try {
      await transaction.insert('temp', 'item1', Buffer.from('data1'));
      await transaction.insert('temp', 'item2', Buffer.from('data2'));

      // Count items before commit
      const countBefore = await transaction.count('temp');
      console.log('Items in transaction:', countBefore);

      // Commit the transaction
      await transaction.close(true);

      // Verify items are persisted
      const verifySession = await store.startSession('profile1');
      const countAfter = await verifySession.count('temp');
      console.log('Items after commit:', countAfter);
      await verifySession.close();

    } catch (error) {
      // Rollback on error
      await transaction.close(false);
      throw error;
    }

    // Clean up sessions
    await session1.close();
    await session2.close();

    // Copy store to a new location
    console.log('\nCopying store...');
    const backupStore = await store.copy('sqlite://backup-store.db', 'raw', 'backup-key');
    console.log('Store copied successfully');
    await backupStore.close();

    await store.close();
    console.log('Advanced example completed successfully');

  } catch (error) {
    console.error('Error in advanced example:', error);
    process.exit(1);
  }
}

if (require.main === module) {
  advancedExample();
}

module.exports = { advancedExample };