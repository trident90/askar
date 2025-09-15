/**
 * Aries Askar Java Wrapper
 *
 * <p>This package provides Java bindings for the Aries Askar secure storage library.
 * Askar provides encrypted storage with support for key management, digital signatures,
 * and cryptographic operations.
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@link org.hyperledger.aries.askar.Store} - Main store interface</li>
 *   <li>{@link org.hyperledger.aries.askar.Session} - Database session for operations</li>
 *   <li>{@link org.hyperledger.aries.askar.Key} - Cryptographic key management</li>
 *   <li>{@link org.hyperledger.aries.askar.Entry} - Data entry representation</li>
 *   <li>{@link org.hyperledger.aries.askar.KeyEntry} - Key entry representation</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Provision a new store
 * Store store = Store.provision("sqlite:///tmp/test.db", "raw", null, null, false);
 *
 * // Open a session and perform operations
 * try (Session session = store.session().open()) {
 *     // Insert data
 *     session.insert("category", "name", "value".getBytes(), null, null);
 *
 *     // Fetch data
 *     Entry entry = session.fetch("category", "name", false);
 *     System.out.println(new String(entry.getValue()));
 * }
 *
 * // Generate and store a key
 * Key key = Key.generate(KeyAlgorithm.ED25519, false);
 * try (Session session = store.session().open()) {
 *     session.insertKey("my-key", key, "Test key", null, null);
 * }
 *
 * store.close();
 * }</pre>
 *
 * @since 0.4.5
 */
package org.hyperledger.aries.askar;