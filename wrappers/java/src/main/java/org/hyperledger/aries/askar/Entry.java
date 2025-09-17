package org.hyperledger.aries.askar;

import java.util.Map;

/**
 * Represents a single entry retrieved from the store.
 * Simplified version without external dependencies.
 */
public class Entry {

    private final String category;
    private final String name;
    private final byte[] value;
    private final Map<String, Object> tags;

    /**
     * Constructor for Entry.
     * @param category The entry category
     * @param name The entry name
     * @param value The entry value
     * @param tags The entry tags
     */
    public Entry(String category, String name, byte[] value, Map<String, Object> tags) {
        this.category = category;
        this.name = name;
        this.value = value != null ? value.clone() : new byte[0];
        this.tags = tags;
    }

    /**
     * Get the entry category.
     * @return The category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Get the entry name.
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the entry value as bytes.
     * @return The value bytes
     */
    public byte[] getValue() {
        return value != null ? value.clone() : new byte[0];
    }

    /**
     * Get the entry value as a string.
     * @return The value as a UTF-8 string
     */
    public String getValueString() {
        return value != null ? new String(value) : "";
    }

    /**
     * Get the entry value as JSON string.
     * Note: For actual JSON parsing, use external JSON libraries as needed.
     * @return The value as a JSON string
     */
    public String getValueJsonString() {
        return getValueString();
    }

    /**
     * Get the entry tags.
     * @return The tags map
     */
    public Map<String, Object> getTags() {
        return tags;
    }

    @Override
    public String toString() {
        return String.format("Entry{category='%s', name='%s', valueLength=%d, tags=%s}",
                category, name, value != null ? value.length : 0, tags);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Entry entry = (Entry) obj;

        if (!category.equals(entry.category)) return false;
        if (!name.equals(entry.name)) return false;
        if (!java.util.Arrays.equals(value, entry.value)) return false;
        return tags != null ? tags.equals(entry.tags) : entry.tags == null;
    }

    @Override
    public int hashCode() {
        int result = category.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + java.util.Arrays.hashCode(value);
        result = 31 * result + (tags != null ? tags.hashCode() : 0);
        return result;
    }
}