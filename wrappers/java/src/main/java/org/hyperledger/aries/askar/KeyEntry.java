package org.hyperledger.aries.askar;

import java.util.Map;

/**
 * Represents a key entry retrieved from the store.
 */
public class KeyEntry {

    private final String algorithm;
    private final String name;
    private final String metadata;
    private final Map<String, Object> tags;
    private final Key key;

    /**
     * Constructor for KeyEntry.
     * @param algorithm The key algorithm
     * @param name The key name
     * @param metadata The key metadata
     * @param tags The key tags
     * @param key The actual key instance
     */
    public KeyEntry(String algorithm, String name, String metadata, Map<String, Object> tags, Key key) {
        this.algorithm = algorithm;
        this.name = name;
        this.metadata = metadata;
        this.tags = tags;
        this.key = key;
    }

    /**
     * Get the key algorithm.
     * @return The algorithm
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Get the key name.
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the key metadata.
     * @return The metadata
     */
    public String getMetadata() {
        return metadata;
    }

    /**
     * Get the key tags.
     * @return The tags map
     */
    public Map<String, Object> getTags() {
        return tags;
    }

    /**
     * Get the key instance.
     * @return The key
     */
    public Key getKey() {
        return key;
    }

    @Override
    public String toString() {
        return String.format("KeyEntry{algorithm='%s', name='%s', metadata='%s', tags=%s}",
                algorithm, name, metadata, tags);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        KeyEntry keyEntry = (KeyEntry) obj;

        if (!algorithm.equals(keyEntry.algorithm)) return false;
        if (!name.equals(keyEntry.name)) return false;
        if (metadata != null ? !metadata.equals(keyEntry.metadata) : keyEntry.metadata != null) return false;
        return tags != null ? tags.equals(keyEntry.tags) : keyEntry.tags == null;
    }

    @Override
    public int hashCode() {
        int result = algorithm.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        result = 31 * result + (tags != null ? tags.hashCode() : 0);
        return result;
    }
}