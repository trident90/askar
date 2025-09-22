const { Store, LocalKey, KeyAlg, Askar } = require('../lib');

async function basicExample() {
  try {
    console.log('Aries Askar JavaScript Wrapper Example');
    console.log('Version:', Askar.getVersion());

    // Set up logging
    Askar.setDefaultLogger();

    // Create a test store
    const storeUri = 'sqlite://:memory:';
    const store = await Store.provision(storeUri, 'raw', 'test-key');
    console.log('Store provisioned successfully');

    // Start a session
    const session = await store.startSession();
    console.log('Session started');

    // Insert some test data
    await session.insert('category1', 'test-entry', Buffer.from('test data'), {
      tag1: 'value1',
      tag2: 'value2'
    });
    console.log('Data inserted');

    // Fetch the data back
    const entry = await session.fetch('category1', 'test-entry');
    if (entry) {
      console.log('Retrieved entry:', {
        category: entry.category,
        name: entry.name,
        value: entry.value.toString(),
        tags: entry.tags
      });
    }

    // Generate a key
    const key = LocalKey.generate(KeyAlg.Ed25519);
    console.log('Generated key with algorithm:', key.getAlgorithm());

    // Get JWK representation
    const jwkPublic = key.getJwkPublic();
    console.log('Public JWK:', JSON.parse(jwkPublic));

    // Store the key
    await session.insertKey(key, 'test-key', 'Test key metadata', {
      purpose: 'signing'
    });
    console.log('Key stored');

    // Fetch the key back
    const keyEntry = await session.fetchKey('test-key');
    if (keyEntry) {
      console.log('Retrieved key entry:', {
        name: keyEntry.name,
        algorithm: keyEntry.algorithm,
        metadata: keyEntry.metadata,
        tags: keyEntry.tags
      });
    }

    // Test signing
    const message = Buffer.from('Hello, Askar!');
    const signature = key.signMessage(message);
    const isValid = key.verifySignature(message, signature);
    console.log('Signature valid:', isValid);

    // Test AEAD encryption (for symmetric keys)
    try {
      const aeadKey = LocalKey.generate(KeyAlg.A256Gcm);
      const plaintext = 'Secret message';
      const encrypted = aeadKey.aeadEncrypt(plaintext);
      const decrypted = aeadKey.aeadDecrypt(
        encrypted.ciphertext,
        encrypted.nonce,
        encrypted.tag
      );
      console.log('AEAD encryption/decryption successful:', decrypted.toString() === plaintext);
    } catch (error) {
      console.log('AEAD test skipped:', error.message);
    }

    // Clean up
    await session.close(true);
    await store.close();
    console.log('Example completed successfully');

  } catch (error) {
    console.error('Error:', error);
    process.exit(1);
  }
}

if (require.main === module) {
  basicExample();
}

module.exports = { basicExample };