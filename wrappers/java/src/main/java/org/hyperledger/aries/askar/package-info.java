/**
 * Aries Askar Java Wrapper - Pure JNI Implementation
 *
 * <p>This package provides Java bindings for the Aries Askar secure storage library
 * using a pure JNI (Java Native Interface) approach without external dependencies.
 * Askar provides encrypted storage with support for key management, digital signatures,
 * and cryptographic operations.
 *
 * <h2>Main Classes</h2>
 * <ul>
 *   <li>{@link org.hyperledger.aries.askar.StoreJNI} - Main store interface (JNI-based)</li>
 *   <li>{@link org.hyperledger.aries.askar.StoreJNI.SessionJNI} - Database session for operations</li>
 *   <li>{@link org.hyperledger.aries.askar.AskarNative} - Native method declarations</li>
 *   <li>{@link org.hyperledger.aries.askar.Entry} - Data entry representation</li>
 *   <li>{@link org.hyperledger.aries.askar.KeyEntry} - Key entry representation</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Provision a new store
 * try (StoreJNI store = StoreJNI.provision("sqlite:///tmp/test.db", "raw", null, null, false)) {
 *     // Open a session and perform operations
 *     try (StoreJNI.SessionJNI session = store.session().open()) {
 *         // Insert data
 *         session.insert("category", "name", "value".getBytes(), null, null);
 *
 *         // Fetch data
 *         Entry entry = session.fetch("category", "name", false);
 *         if (entry != null) {
 *             System.out.println(new String(entry.getValue()));
 *         }
 *     }
 * }
 *
 * // Generate cryptographic keys using native methods
 * long keyHandle = AskarNative.keyGenerate("ed25519", null, false);
 * byte[] publicKey = AskarNative.keyGetPublicBytes(keyHandle);
 * AskarNative.keyFree(keyHandle);
 * }</pre>
 *
 * <h2>Architecture</h2>
 * <p>This implementation uses pure JNI without JNA, Jackson, SLF4J, or other external dependencies.
 * JSON parsing for tags is handled with simple built-in methods. All native library interactions
 * go through the {@link org.hyperledger.aries.askar.AskarNative} class.</p>
 *
 * @since 0.4.5
 */
package org.hyperledger.aries.askar;