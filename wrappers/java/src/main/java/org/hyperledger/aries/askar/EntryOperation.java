package org.hyperledger.aries.askar;

/**
 * Entry operation types for database operations.
 * Values must match Rust EntryOperation enum: 0=INSERT, 1=REPLACE, 2=REMOVE
 */
public enum EntryOperation {
    INSERT((byte)0),
    REPLACE((byte)1),
    REMOVE((byte)2);
    
    private final byte value;
    
    EntryOperation(byte value) {
        this.value = value;
    }
    
    public byte getValue() {
        return value;
    }
}